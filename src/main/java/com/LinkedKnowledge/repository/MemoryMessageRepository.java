package com.LinkedKnowledge.repository;

import com.LinkedKnowledge.entity.MemoryMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MemoryMessageRepository extends JpaRepository<MemoryMessage, Long> {
    List<MemoryMessage> findTop80ByUserIdOrderByCreatedAtDesc(Long userId);

    @Transactional
    void deleteByUserId(Long userId);
}
