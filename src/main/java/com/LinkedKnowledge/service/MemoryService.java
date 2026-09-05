package com.LinkedKnowledge.service;

import com.LinkedKnowledge.common.LlmClient;
import com.LinkedKnowledge.dto.MemoryChatResponse;
import com.LinkedKnowledge.dto.NodeSummary;
import com.LinkedKnowledge.entity.KnowledgeNode;
import com.LinkedKnowledge.entity.MemoryFact;
import com.LinkedKnowledge.entity.MemoryMessage;
import com.LinkedKnowledge.repository.KnowledgeNodeRepository;
import com.LinkedKnowledge.repository.MemoryFactRepository;
import com.LinkedKnowledge.repository.MemoryMessageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryService {

    private static final String EXTRACT_PROMPT =
            "你是个人知识库的记忆整理助手。根据笔记抽出 3 到 8 条独立、客观、可检索的事实。\n" +
            "要求：\n" +
            "1. 只输出 JSON 数组，元素是字符串，不要 markdown。\n" +
            "2. 每条一句话，包含实体或结论，不要空话。\n" +
            "3. 不要编造笔记里没有的内容。";

    private static final String CHAT_PROMPT =
            "你是 LinkedKnowledge 里的个人知识库助手。用户的笔记和已记住的事实是唯一依据。\n" +
            "规则：\n" +
            "1. 优先用「相关笔记」和「已记住的事实」回答，并点出笔记标题。\n" +
            "2. 不知道就说不知道，不要编。\n" +
            "3. 用简洁中文。如果用户说「记住……」，确认你会记住，并以后都当偏好/事实用。\n" +
            "4. 可以建议用 [[笔记标题]] 互相链接。";

    private final LlmClient llmClient;
    private final KnowledgeNodeRepository nodeRepository;
    private final MemoryFactRepository factRepository;
    private final MemoryMessageRepository messageRepository;
    private final PlatformTransactionManager transactionManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async
    public void retainNoteAsync(Long nodeId) {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> retainNote(nodeId));
        } catch (Exception e) {
            log.warn("抽取笔记记忆失败 nodeId={}: {}", nodeId, e.getMessage());
        }
    }

    public void retainNote(Long nodeId) {
        KnowledgeNode node = nodeRepository.findById(nodeId).orElse(null);
        if (node == null || node.getUserId() == null) return;
        String title = node.getTitle() == null ? "" : node.getTitle();
        String content = node.getContent() == null ? "" : node.getContent();
        List<String> facts = extractFacts(title, content);
        factRepository.deleteByUserIdAndNodeId(node.getUserId(), nodeId);
        for (String text : facts) {
            MemoryFact f = new MemoryFact();
            f.setUserId(node.getUserId());
            f.setNodeId(nodeId);
            f.setKind("NOTE");
            f.setContent(text);
            f.setCreatedAt(LocalDateTime.now());
            factRepository.save(f);
        }
    }

    public List<MemoryFact> facts(Long userId, String q) {
        if (q == null || q.isBlank()) {
            return factRepository.findTop40ByUserIdOrderByCreatedAtDesc(userId);
        }
        return factRepository.search(userId, q.trim());
    }

    public List<MemoryMessage> history(Long userId) {
        List<MemoryMessage> newestFirst = messageRepository.findTop80ByUserIdOrderByCreatedAtDesc(userId);
        List<MemoryMessage> chrono = new ArrayList<>(newestFirst);
        java.util.Collections.reverse(chrono);
        return chrono;
    }

    @Transactional
    public void clearChat(Long userId) {
        messageRepository.deleteByUserId(userId);
    }

    @Transactional
    public void deleteFactsForNode(Long nodeId) {
        factRepository.deleteByNodeId(nodeId);
    }

    public MemoryChatResponse chat(Long userId, String message, Long nodeId) {
        String q = message == null ? "" : message.trim();
        if (q.isEmpty()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            saveMessage(userId, "user", q, nodeId);
            if (looksLikeRemember(q)) {
                MemoryFact pref = new MemoryFact();
                pref.setUserId(userId);
                pref.setNodeId(nodeId);
                pref.setKind("PREF");
                pref.setContent(stripRememberPrefix(q));
                pref.setCreatedAt(LocalDateTime.now());
                factRepository.save(pref);
            }
        });

        List<KnowledgeNode> notes = retrieveNotes(userId, q, nodeId);
        List<MemoryFact> facts = retrieveFacts(userId, q);
        List<MemoryMessage> recent = history(userId);
        if (recent.size() > 16) {
            recent = recent.subList(recent.size() - 16, recent.size());
        }

        String reply;
        if (llmClient.isConfigured()) {
            try {
                reply = llmClient.chat(CHAT_PROMPT, buildUserPrompt(q, nodeId, userId, notes, facts, recent));
            } catch (Exception e) {
                log.warn("记忆对话调用模型失败: {}", e.getMessage());
                reply = fallbackReply(notes, facts);
            }
        } else {
            reply = fallbackReply(notes, facts) + "\n\n（未配置大模型密钥，以上是库内检索结果。）";
        }
        if (reply == null || reply.isBlank()) {
            reply = "我这边没有检索到足够内容。";
        }
        final String toSave = reply;
        tx.executeWithoutResult(status -> saveMessage(userId, "assistant", toSave, nodeId));

        MemoryChatResponse out = new MemoryChatResponse();
        out.setReply(reply);
        List<NodeSummary> sources = new ArrayList<>();
        for (KnowledgeNode n : notes) {
            sources.add(new NodeSummary(n.getId(), n.getTitle(), snippet(n.getContent(), 160)));
        }
        out.setSources(sources);
        return out;
    }

    private List<String> extractFacts(String title, String content) {
        List<String> fallback = heuristicFacts(title, content);
        if (!llmClient.isConfigured() || content == null || content.isBlank()) {
            return fallback;
        }
        try {
            String raw = llmClient.chat(EXTRACT_PROMPT,
                    "标题：" + title + "\n正文：\n" + clip(content, 4000));
            List<String> parsed = parseFactList(raw);
            return parsed.isEmpty() ? fallback : parsed;
        } catch (Exception e) {
            log.warn("LLM 抽事实失败，用原文摘要: {}", e.getMessage());
            return fallback;
        }
    }

    private List<String> heuristicFacts(String title, String content) {
        List<String> out = new ArrayList<>();
        if (title != null && !title.isBlank()) {
            out.add("库中有一篇笔记《" + title.trim() + "》。");
        }
        String body = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        if (!body.isEmpty()) {
            out.add("《" + title + "》要点：" + clip(body, 180));
        }
        return out;
    }

    private List<String> parseFactList(String raw) {
        String cleaned = stripCodeFence(raw);
        List<String> out = new ArrayList<>();
        try {
            JsonNode node = objectMapper.readTree(cleaned);
            JsonNode arr = node.isArray() ? node : node.get("facts");
            if (arr != null && arr.isArray()) {
                for (JsonNode item : arr) {
                    String s = item.isTextual() ? item.asText() : item.toString();
                    s = s == null ? "" : s.trim();
                    if (!s.isEmpty()) out.add(clip(s, 300));
                    if (out.size() >= 8) break;
                }
            }
        } catch (Exception e) {
            log.debug("事实 JSON 解析失败: {}", e.getMessage());
        }
        return out;
    }

    private List<KnowledgeNode> retrieveNotes(Long userId, String q, Long nodeId) {
        Set<Long> seen = new LinkedHashSet<>();
        List<KnowledgeNode> out = new ArrayList<>();
        if (nodeId != null) {
            nodeRepository.findById(nodeId).ifPresent(n -> {
                if (userId.equals(n.getUserId())) {
                    seen.add(n.getId());
                    out.add(n);
                }
            });
        }
        List<KnowledgeNode> hits = nodeRepository.searchVault(userId, clip(q, 80));
        for (KnowledgeNode n : hits) {
            if (seen.add(n.getId())) out.add(n);
            if (out.size() >= 6) break;
        }
        return out;
    }

    private List<MemoryFact> retrieveFacts(Long userId, String q) {
        List<MemoryFact> hits = factRepository.search(userId, clip(q, 80));
        if (!hits.isEmpty()) {
            return hits.size() > 12 ? hits.subList(0, 12) : hits;
        }
        return factRepository.findTop40ByUserIdOrderByCreatedAtDesc(userId)
                .stream().limit(12).toList();
    }

    private String buildUserPrompt(String q, Long nodeId, Long userId,
                                   List<KnowledgeNode> notes, List<MemoryFact> facts,
                                   List<MemoryMessage> recent) {
        StringBuilder sb = new StringBuilder();
        if (nodeId != null) {
            nodeRepository.findById(nodeId).ifPresent(n -> {
                if (userId.equals(n.getUserId())) {
                    sb.append("## 当前正在看的笔记\n标题：").append(n.getTitle())
                            .append("\n").append(clip(n.getContent(), 1200)).append("\n\n");
                }
            });
        }
        sb.append("## 相关笔记\n");
        if (notes.isEmpty()) sb.append("（无）\n");
        for (KnowledgeNode n : notes) {
            sb.append("- [[")
                    .append(n.getTitle())
                    .append("]] (id=")
                    .append(n.getId())
                    .append(")\n")
                    .append(clip(n.getContent(), 400))
                    .append("\n");
        }
        sb.append("\n## 已记住的事实\n");
        if (facts.isEmpty()) sb.append("（无）\n");
        for (MemoryFact f : facts) {
            sb.append("- ").append(f.getContent()).append("\n");
        }
        sb.append("\n## 最近对话\n");
        for (MemoryMessage m : recent) {
            if ("user".equals(m.getRole()) && q.equals(m.getContent()) && m == recent.get(recent.size() - 1)) {
                continue;
            }
            sb.append(m.getRole()).append(": ").append(clip(m.getContent(), 400)).append("\n");
        }
        sb.append("\n## 用户问题\n").append(q);
        return sb.toString();
    }

    private String fallbackReply(List<KnowledgeNode> notes, List<MemoryFact> facts) {
        StringBuilder sb = new StringBuilder();
        if (!notes.isEmpty()) {
            sb.append("库里和这个问题相关的笔记：\n");
            for (KnowledgeNode n : notes) {
                sb.append("- [[").append(n.getTitle()).append("]]\n");
            }
        }
        if (!facts.isEmpty()) {
            sb.append("\n已记住：\n");
            for (MemoryFact f : facts.subList(0, Math.min(6, facts.size()))) {
                sb.append("- ").append(f.getContent()).append("\n");
            }
        }
        if (sb.isEmpty()) {
            return "知识库里还没有足够相关的笔记。先写几篇并保存，我会开始记住。";
        }
        return sb.toString().trim();
    }

    private void saveMessage(Long userId, String role, String content, Long nodeId) {
        MemoryMessage m = new MemoryMessage();
        m.setUserId(userId);
        m.setRole(role);
        m.setContent(content);
        m.setRelatedNodeId(nodeId);
        m.setCreatedAt(LocalDateTime.now());
        messageRepository.save(m);
    }

    private static boolean looksLikeRemember(String q) {
        return q.startsWith("记住") || q.startsWith("请记住") || q.contains("请记住：") || q.contains("记住：");
    }

    private static String stripRememberPrefix(String q) {
        return q.replaceFirst("^(请)?记住[：:]?", "").trim();
    }

    private static String clip(String s, int max) {
        if (s == null) return "";
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    private static String snippet(String s, int max) {
        if (s == null) return "";
        return clip(s.replaceAll("\\s+", " "), max);
    }

    private static String stripCodeFence(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        Pattern p = Pattern.compile("^```(?:json)?\\s*\\n?(.*?)\\n?```$", Pattern.DOTALL);
        Matcher m = p.matcher(s);
        return m.matches() ? m.group(1).trim() : s;
    }
}
