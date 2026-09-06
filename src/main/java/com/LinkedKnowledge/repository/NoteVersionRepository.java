package com.LinkedKnowledge.repository;

import com.LinkedKnowledge.entity.NoteVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface NoteVersionRepository extends JpaRepository<NoteVersion, Long> {
    List<NoteVersion> findByUserIdAndNodeIdOrderByCreatedAtDesc(Long userId, Long nodeId);
    Optional<NoteVersion> findByIdAndUserId(Long id, Long userId);
    @Transactional
    void deleteByNodeId(Long nodeId);
}
