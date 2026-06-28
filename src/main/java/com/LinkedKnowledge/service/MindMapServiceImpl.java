package com.LinkedKnowledge.service;

import com.LinkedKnowledge.common.LlmClient;
import com.LinkedKnowledge.dto.MindMapNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MindMapServiceImpl implements MindMapService {

    private final LlmClient llmClient;
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
    public MindMapNode generateMindMap(String topic, String context) {
        String userPrompt;
        if (context == null || context.isBlank()) {
            userPrompt = "主题：" + topic + "\n请生成思维导图。";
        } else {
            userPrompt = "主题：" + topic + "\n上下文：\n" + context + "\n请根据以上内容生成思维导图。";
        }

        log.info("调用大模型生成导图 topic={}", topic);
        String raw = llmClient.chat(SYSTEM_PROMPT, userPrompt);
        log.debug("大模型原始返回: {}", raw);

        // 容错：万一模型多嘴加了 ```json 之类，剥掉
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
        // 去掉 ```json ... ``` 或 ``` ... ```
        Pattern p = Pattern.compile("^```(?:json)?\\s*\\n?(.*?)\\n?```$", Pattern.DOTALL);
        Matcher m = p.matcher(s);
        if (m.matches()) {
            return m.group(1).trim();
        }
        return s;
    }
}