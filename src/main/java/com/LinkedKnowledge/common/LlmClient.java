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


    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String chat(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
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
                // 显式 UTF-8 解码响应体，避免中文字符被当 GBK 处理
                String responseBody = "";
                if (response.body() != null) {
                    byte[] bytes = response.body().bytes();
                    // === DEBUG: 写 raw bytes 到文件，绕过 logback 缓冲 ===
                    try {
                        java.nio.file.Files.write(
                                java.nio.file.Paths.get("/tmp/llm-raw-response.bin"),
                                bytes
                        );
                    } catch (Exception ignore) {}
                    responseBody = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                    // === DEBUG: 写 decoded body 到文件 ===
                    try {
                        java.nio.file.Files.writeString(
                                java.nio.file.Paths.get("/tmp/llm-raw-response.txt"),
                                responseBody
                        );
                    } catch (Exception ignore) {}
                }
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
            log.error("=====DEBUG LlmClient error=====", e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("大模型调用异常: " + e.getMessage(), e);
        }
    }
}