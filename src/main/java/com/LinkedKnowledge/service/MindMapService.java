package com.LinkedKnowledge.service;

import com.LinkedKnowledge.dto.MindMapNode;

public interface MindMapService {
    /**
     * 根据主题 + 可选上下文，生成思维导图树形结构
     */
    MindMapNode generateMindMap(String topic, String context);
}