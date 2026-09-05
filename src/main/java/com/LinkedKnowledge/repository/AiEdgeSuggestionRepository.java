package com.LinkedKnowledge.repository;

import com.LinkedKnowledge.entity.AiEdgeSuggestion;
import com.LinkedKnowledge.entity.AiSuggestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiEdgeSuggestionRepository extends JpaRepository<AiEdgeSuggestion, Long> {

    List<AiEdgeSuggestion> findByUserIdAndStatus(Long userId, AiSuggestionStatus status);

    List<AiEdgeSuggestion> findBySourceNodeIdAndStatus(Long sourceNodeId, AiSuggestionStatus status);

    /** 检查是否已有同向建议(避免重复 AI 推荐) */
    boolean existsBySourceNodeIdAndTargetNodeIdAndStatus(
            Long sourceNodeId, Long targetNodeId, AiSuggestionStatus status);

    void deleteBySourceNodeIdOrTargetNodeId(Long sourceNodeId, Long targetNodeId);
}
