package com.LinkedKnowledge.repository;

import com.LinkedKnowledge.entity.MemoryFact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MemoryFactRepository extends JpaRepository<MemoryFact, Long> {
    List<MemoryFact> findTop40ByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT f FROM MemoryFact f WHERE f.userId = :userId AND LOWER(f.content) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY f.createdAt DESC")
    List<MemoryFact> search(@Param("userId") Long userId, @Param("q") String q);

    @Transactional
    void deleteByUserIdAndNodeId(Long userId, Long nodeId);

    @Transactional
    void deleteByNodeId(Long nodeId);
}
