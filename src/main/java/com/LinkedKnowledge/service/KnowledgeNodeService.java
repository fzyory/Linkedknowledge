package com.LinkedKnowledge.service;

import com.LinkedKnowledge.entity.KnowledgeNode;
import com.LinkedKnowledge.entity.NodeType;

import java.util.List;

public interface KnowledgeNodeService {
    KnowledgeNode createNode(KnowledgeNode node);

    KnowledgeNode updateNode(Long id, KnowledgeNode updateNode);

    void deleteNode(Long id);

    KnowledgeNode getNodeById(Long id);

    List<KnowledgeNode> getNodesByType(NodeType type);

    List<KnowledgeNode> getAllNodes();

    List<KnowledgeNode> getRootNodes();

    List<KnowledgeNode> searchByTitle(String keyword);
}
