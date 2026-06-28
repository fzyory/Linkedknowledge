package com.LinkedKnowledge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 思维导图节点 DTO —— 用于前端 mind-elixir 渲染
 * 树形结构，id 用于拖拽定位，topic 是显示文本
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MindMapNode {
    private String id;
    private String topic;
    private List<MindMapNode> children = new ArrayList<>();

    public MindMapNode(String id, String topic) {
        this.id = id;
        this.topic = topic;
    }
}