package com.LinkedKnowledge.repository;

import com.LinkedKnowledge.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Attachment> findByIdAndUserId(Long id, Long userId);
    @Transactional
    void deleteByNodeId(Long nodeId);
}
