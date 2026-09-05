package com.LinkedKnowledge.repository;

import com.LinkedKnowledge.entity.NoteTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteTemplateRepository extends JpaRepository<NoteTemplate, Long> {
    List<NoteTemplate> findByUserIdOrderByNameAsc(Long userId);
    Optional<NoteTemplate> findByIdAndUserId(Long id, Long userId);
}
