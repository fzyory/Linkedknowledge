package com.LinkedKnowledge.repository;

import com.LinkedKnowledge.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    List<Bookmark> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Bookmark> findByIdAndUserId(Long id, Long userId);
    Optional<Bookmark> findByUserIdAndNodeId(Long userId, Long nodeId);
    @Transactional
    void deleteByNodeId(Long nodeId);
}
