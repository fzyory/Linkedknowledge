package com.LinkedKnowledge.service;

import com.LinkedKnowledge.entity.KnowledgeNode;
import com.LinkedKnowledge.entity.NodeType;

import java.util.List;

public interface KnowledgeNodeService {
    KnowledgeNode createNode(KnowledgeNode node);

    KnowledgeNode updateNode(Long id, KnowledgeNode updateNode, Long userId);

    void deleteNode(Long id, Long userId);

    KnowledgeNode getNodeById(Long id, Long userId);

    List<KnowledgeNode> getNodesByType(NodeType type, Long userId);

    List<KnowledgeNode> getAllNodes(Long userId);

    List<KnowledgeNode> getRootNodes(Long userId);

    List<KnowledgeNode> searchByTitle(String keyword, Long userId);

    /** 节点不存在或不属于该用户时抛 RuntimeException */
    void assertOwned(Long nodeId, Long userId);
}
