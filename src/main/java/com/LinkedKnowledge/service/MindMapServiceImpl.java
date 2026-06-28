package com.LinkedKnowledge.service;

import com.LinkedKnowledge.common.LlmClient;
import com.LinkedKnowledge.dto.MindMapNode;
import com.LinkedKnowledge.entity.MindMap;
import com.LinkedKnowledge.repository.MindMapRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MindMapServiceImpl implements MindMapService {

    private final LlmClient llmClient;
    private final MindMapRepository mindMapRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Prompt 关键：强制模型只输出 JSON，不解释不 markdown
     */
    private static final String SYSTEM_PROMPT =
            "你是一个思维导图生成助手。\n" +
            "你的唯一任务：根据用户提供的主题和上下文，生成一个树形 JSON 结构。\n" +
            "严格要求：\n" +
            "1. 只输出 JSON，不要任何解释、不要 markdown 代码块标记、不要前后缀文字。\n" +
            "2. JSON 格式：\n" +
            "{\"id\":\"root\",\"topic\":\"<主题>\",\"children\":[\n" +
            "  {\"id\":\"n1\",\"topic\":\"<一级分支1>\",\"children\":[\n" +
            "    {\"id\":\"n1-1\",\"topic\":\"<二级子节点>\",\"children\":[]}\n" +
            "  ]},\n" +
            "  {\"id\":\"n2\",\"topic\":\"<一级分支2>\",\"children\":[]}\n" +
            "]}\n" +
            "3. 一级分支不超过 6 个，每个分支下二级不超过 4 个，二级以上酌情。\n" +
            "4. id 命名规则：root, n1, n1-1, n1-1-1 ... 用短横线分隔层级。\n" +
            "5. topic 控制在 12 个汉字以内。\n";

    @Override
    public MindMap generateAndSave(Long userId, String topic, String context) {
        // 1. 调大模型生成树
        MindMapNode rootNode = generateTree(topic, context);

        // 2. 序列化为 JSON
        String treeJson;
        try {
            treeJson = objectMapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            log.error("序列化导图失败", e);
            throw new RuntimeException("导图序列化失败: " + e.getMessage());
        }

        // 3. 存库
        MindMap mindMap = new MindMap();
        mindMap.setTitle(topic);
        mindMap.setTopic(topic);
        mindMap.setContext(context);
        mindMap.setTreeJson(treeJson);
        mindMap.setUserId(userId);
        MindMap saved = mindMapRepository.save(mindMap);
        log.info("思维导图已存库 id={} topic={} userId={}", saved.getId(), topic, userId);
        return saved;
    }

    @Override
    public List<MindMap> listByUser(Long userId) {
        return mindMapRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    @Override
    public MindMap getById(Long userId, Long id) {
        MindMap mindMap = mindMapRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("导图不存在 id=" + id));
        // 权限校验：只能看自己的
        if (!mindMap.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该导图");
        }
        return mindMap;
    }

    @Override
    public MindMapNode parseTreeJson(String treeJson) {
        try {
            return objectMapper.readValue(treeJson, MindMapNode.class);
        } catch (Exception e) {
            throw new RuntimeException("导图 JSON 解析失败: " + e.getMessage());
        }
    }

    // ===== 内部方法 =====

    private MindMapNode generateTree(String topic, String context) {
        String userPrompt;
        if (context == null || context.isBlank()) {
            userPrompt = "主题：" + topic + "\n请生成思维导图。";
        } else {
            userPrompt = "主题：" + topic + "\n上下文：\n" + context + "\n请根据以上内容生成思维导图。";
        }

        log.info("调用大模型生成导图 topic={}", topic);
        String raw = llmClient.chat(SYSTEM_PROMPT, userPrompt);
        log.debug("大模型原始返回: {}", raw);

        String cleaned = stripCodeFence(raw);

        try {
            JsonNode node = objectMapper.readTree(cleaned);
            return objectMapper.treeToValue(node, MindMapNode.class);
        } catch (Exception e) {
            log.error("解析大模型返回失败 raw={}", raw, e);
            throw new RuntimeException("大模型返回格式无法解析为导图: " + e.getMessage());
        }
    }

    private String stripCodeFence(String raw) {
        String s = raw.trim();
        Pattern p = Pattern.compile("^```(?:json)?\\s*\\n?(.*?)\\n?```$", Pattern.DOTALL);
        Matcher m = p.matcher(s);
        if (m.matches()) {
            return m.group(1).trim();
        }
        return s;
    }
}