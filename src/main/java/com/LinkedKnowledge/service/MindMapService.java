package com.LinkedKnowledge.service;

import com.LinkedKnowledge.dto.MindMapNode;
import com.LinkedKnowledge.entity.MindMap;

import java.util.List;

public interface MindMapService {
    /**
     * 生成思维导图并自动存库
     * @return 生成的导图（带 id）
     */
    MindMap generateAndSave(Long userId, String topic, String context);

    /**
     * 查询某用户所有导图
     */
    List<MindMap> listByUser(Long userId);

    /**
     * 查询单个导图详情
     */
    MindMap getById(Long userId, Long id);

    /**
     * 解析导图 JSON 为树形结构（前端渲染用）
     */
    MindMapNode parseTreeJson(String treeJson);
}