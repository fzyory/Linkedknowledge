package com.LinkedKnowledge.repository;

import com.LinkedKnowledge.entity.KnowledgeNode;
import com.LinkedKnowledge.entity.NodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeNodeRepository extends JpaRepository<KnowledgeNode, Long> {

    // Spring Data JPA 会根据方法名自动生成 SQL：
    // SELECT * FROM knowledge_node WHERE title LIKE '%keyword%'
    List<KnowledgeNode> findByTitleContaining(String keyword);

    // SELECT * FROM knowledge_node WHERE type = ?
    List<KnowledgeNode> findByType(NodeType type);

    // SELECT * FROM knowledge_node WHERE parent_id IS NULL
    List<KnowledgeNode> findByParentIsNull();
}
