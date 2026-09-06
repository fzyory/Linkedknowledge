package com.LinkedKnowledge.service;

import com.LinkedKnowledge.ai.SimpleTfidfSimilarity;
import com.LinkedKnowledge.ai.SimpleTfidfSimilarity.Suggestion;
import com.LinkedKnowledge.entity.AiEdgeSuggestion;
import com.LinkedKnowledge.entity.AiSuggestionStatus;
import com.LinkedKnowledge.entity.KnowledgeNode;
import com.LinkedKnowledge.repository.AiEdgeSuggestionRepository;
import com.LinkedKnowledge.repository.KnowledgeNodeRepository;
import com.LinkedKnowledge.repository.NodeEdgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EdgeSuggestionService {

    private final KnowledgeNodeRepository nodeRepository;
    private final NodeEdgeRepository edgeRepository;
    private final AiEdgeSuggestionRepository suggestionRepository;

    /**
     * 异步触发:为某节点算 AI 建议
     * 在 KnowledgeNodeService.createNode / updateNode 末尾调用
     */
    @Async
    @Transactional
    public void suggestRelated(Long sourceNodeId) {
        try {
            KnowledgeNode source = nodeRepository.findById(sourceNodeId).orElse(null);
            if (source == null || source.getUserId() == null) return;

            List<KnowledgeNode> allNodes = nodeRepository.findByUserId(source.getUserId());
            if (allNodes.size() < 2) return;

            Map<Long, String> docs = allNodes.stream()
                    .collect(Collectors.toMap(
                            KnowledgeNode::getId,
                            n -> (n.getTitle() == null ? "" : n.getTitle()) + " " +
                                 (n.getContent() == null ? "" : n.getContent())
                    ));

            List<Suggestion> top = SimpleTfidfSimilarity.findTopSimilar(docs, sourceNodeId, 5);

            for (Suggestion s : top) {
                boolean alreadyAccepted = edgeRepository.findAcceptedBySource(sourceNodeId).stream()
                        .anyMatch(e -> e.getTargetNodeId().equals(s.docId()));
                if (alreadyAccepted) continue;

                boolean rejected = suggestionRepository
                        .existsBySourceNodeIdAndTargetNodeIdAndStatus(
                                sourceNodeId, s.docId(), AiSuggestionStatus.REJECTED);
                if (rejected) continue;

                if (suggestionRepository.existsBySourceNodeIdAndTargetNodeIdAndStatus(
                        sourceNodeId, s.docId(), AiSuggestionStatus.PENDING)) continue;

                AiEdgeSuggestion sug = new AiEdgeSuggestion();
                sug.setSourceNodeId(sourceNodeId);
                sug.setTargetNodeId(s.docId());
                sug.setScore(s.score());
                sug.setSuggestionReason(s.reason());
                sug.setUserId(source.getUserId());
                sug.setStatus(AiSuggestionStatus.PENDING);
                suggestionRepository.save(sug);
            }
        } catch (Exception e) {
            log.error("AI suggestion failed for node {}", sourceNodeId, e);
        }
    }
}
