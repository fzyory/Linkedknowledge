package com.LinkedKnowledge.repository;

import com.LinkedKnowledge.entity.KnowledgeNode;
import com.LinkedKnowledge.entity.NodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeNodeRepository extends JpaRepository<KnowledgeNode, Long> {

    // Spring Data JPA 会根据方法名自动生成 SQL：
    // SELECT * FROM knowledge_node WHERE title LIKE '%keyword%'
    @Query("SELECT DISTINCT n FROM KnowledgeNode n LEFT JOIN FETCH n.tags WHERE n.title LIKE CONCAT('%', :keyword, '%') AND n.userId = :userId")
    List<KnowledgeNode> findByTitleContainingAndUserId(@Param("keyword") String keyword, @Param("userId") Long userId);

    @Query("SELECT DISTINCT n FROM KnowledgeNode n LEFT JOIN FETCH n.tags WHERE n.type = :type AND n.userId = :userId")
    List<KnowledgeNode> findByTypeAndUserId(@Param("type") NodeType type, @Param("userId") Long userId);

    @Query("SELECT DISTINCT n FROM KnowledgeNode n LEFT JOIN FETCH n.tags WHERE n.parent IS NULL AND n.userId = :userId")
    List<KnowledgeNode> findByParentIsNullAndUserId(@Param("userId") Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    List<KnowledgeNode> findByUserId(Long userId);

    Optional<KnowledgeNode> findFirstByTitleAndUserId(String title, Long userId);

    List<KnowledgeNode> findByTitleAndUserId(String title, Long userId);

    @Query("SELECT DISTINCT n FROM KnowledgeNode n LEFT JOIN FETCH n.tags WHERE n.id = :id")
    Optional<KnowledgeNode> findByIdWithTags(@Param("id") Long id);

    @Query("SELECT DISTINCT n FROM KnowledgeNode n LEFT JOIN FETCH n.tags WHERE n.userId = :userId")
    List<KnowledgeNode> findByUserIdWithTags(@Param("userId") Long userId);

    @Query("SELECT DISTINCT n FROM KnowledgeNode n LEFT JOIN FETCH n.tags WHERE n.userId = :userId AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(COALESCE(n.content, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(COALESCE(n.aliases, '')) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<KnowledgeNode> searchVault(@Param("userId") Long userId, @Param("q") String q);

    List<KnowledgeNode> findByUserIdAndFolderPath(Long userId, String folderPath);

    @Query("SELECT n FROM KnowledgeNode n JOIN n.tags t WHERE t.name = :name AND n.userId = :userId")
    List<KnowledgeNode> findByTagNameAndUserId(@Param("name") String name, @Param("userId") Long userId);
}
