package com.LinkedKnowledge.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 大模型 HTTP 客户端 —— 基础设施类
 * 用 OkHttp 直接调 OpenAI 兼容协议（minimax 用同一套）
 *
 * 用法：
 *   String reply = llmClient.chat(systemPrompt, userPrompt);
 *
 * 不解析 JSON，不懂业务，纯转发 + 提取 content 字段
 */
@Slf4j
@Component
public class LlmClient {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${llm.api-key}")
    private String apiKey;

    @Value("${llm.base-url}")
    private String baseUrl;

    @Value("${llm.model}")
    private String model;

    public LlmClient(@Value("${llm.timeout-seconds:60}") long timeoutSeconds) {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 调一次大模型对话，返回模型回复的 content 字段原始字符串
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return 模型回复（可能含 markdown / JSON，调用方自行处理）
     */
    public String chat(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("llm.api-key 未配置（请设置环境变量 MINIMAX_CN_API_KEY）");
        }

        try {
            // 1. 构造请求体
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", model);
            ArrayNode messages = root.putArray("messages");
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                ObjectNode sys = messages.addObject();
                sys.put("role", "system");
                sys.put("content", systemPrompt);
            }
            ObjectNode user = messages.addObject();
            user.put("role", "user");
            user.put("content", userPrompt);
            root.put("temperature", 0.7);

            RequestBody body = RequestBody.create(
                    objectMapper.writeValueAsString(root),
                    MediaType.parse("application/json")
            );

            // 2. 构造 HTTP 请求
            Request request = new Request.Builder()
                    .url(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            // 3. 发送请求
            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    log.error("大模型调用失败 status={} body={}", response.code(), responseBody);
                    throw new RuntimeException("大模型调用失败: HTTP " + response.code() + " - " + responseBody);
                }
                // 4. 解析返回 JSON，提取 content
                JsonNode json = objectMapper.readTree(responseBody);
                JsonNode choices = json.get("choices");
                if (choices == null || !choices.isArray() || choices.isEmpty()) {
                    throw new RuntimeException("大模型返回格式异常: " + responseBody);
                }
                return choices.get(0).get("message").get("content").asText();
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("大模型调用异常: " + e.getMessage(), e);
        }
    }
}