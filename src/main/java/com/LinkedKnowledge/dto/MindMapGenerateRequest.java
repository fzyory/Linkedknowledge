package com.LinkedKnowledge.dto;

import lombok.Data;

/**
 * 思维导图生成请求体
 * topic   必填，要画导图的主题
 * context 可选，附加上下文（用户粘贴的一段文字 / 文章）
 */
@Data
public class MindMapGenerateRequest {
    private String topic;
    private String context;
}