package com.LinkedKnowledge.service;

import com.LinkedKnowledge.entity.KnowledgeNode;
import com.LinkedKnowledge.entity.NodeType;
import com.LinkedKnowledge.repository.KnowledgeNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeNodeServiceImpl implements KnowledgeNodeService{
    private final KnowledgeNodeRepository nodeRepository;
    @Override
    public KnowledgeNode createNode(KnowledgeNode node){
        return nodeRepository.save(node);

    }
    @Override
    public KnowledgeNode updateNode(Long id,KnowledgeNode updatedNode){
        KnowledgeNode existing =getNodeById(id);
        existing.setTitle(updatedNode.getTitle());
        existing.setContent(updatedNode.getContent());
        existing.setStatus(updatedNode.getStatus());
        existing.setType(updatedNode.getType());
        existing.setDifficultyLevel(updatedNode.getDifficultyLevel());
        nodeRepository.save(existing);
        return existing;

    }
    @Override
    public void deleteNode(Long id){
        nodeRepository.deleteById(id);
    }
    @Override
    public KnowledgeNode getNodeById(Long id){
        return nodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("节点不存在"));
    }
    @Override
    public List<KnowledgeNode> getNodesByType(NodeType type){
        return nodeRepository.findByType(type);
    }
    @Override
    public List<KnowledgeNode> getAllNodes(){
        return nodeRepository.findAll();
    }
    @Override
    public List<KnowledgeNode> getRootNodes(){
        return nodeRepository.findByParentIsNull();
    }
    @Override
    public List<KnowledgeNode> searchByTitle(String keyword){
        return nodeRepository.findByTitleContaining(keyword);
    }
}
