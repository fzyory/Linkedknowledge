package com.LinkedKnowledge.controller;

import com.LinkedKnowledge.common.AuthHelper;
import com.LinkedKnowledge.common.Result;
import com.LinkedKnowledge.dto.AiSuggestionWithTitle;
import com.LinkedKnowledge.entity.AiEdgeSuggestion;
import com.LinkedKnowledge.entity.AiSuggestionStatus;
import com.LinkedKnowledge.entity.EdgeStatus;
import com.LinkedKnowledge.entity.EdgeType;
import com.LinkedKnowledge.entity.NodeEdge;
import com.LinkedKnowledge.repository.AiEdgeSuggestionRepository;
import com.LinkedKnowledge.repository.NodeEdgeRepository;
import com.LinkedKnowledge.service.KnowledgeNodeService;
import com.LinkedKnowledge.service.NodeEdgeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/ai-suggestions")
@RequiredArgsConstructor
public class AiEdgeSuggestionController {

    private final AiEdgeSuggestionRepository suggestionRepository;
    private final NodeEdgeRepository edgeRepository;
    private final NodeEdgeMapper edgeMapper;
    private final KnowledgeNodeService nodeService;

    /** 查某节点的所有 PENDING 建议（含源/目标标题） */
    @GetMapping("/node/{nodeId}/pending")
    public Result<List<AiSuggestionWithTitle>> pending(@PathVariable Long nodeId) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        nodeService.assertOwned(nodeId, userId);
        List<AiEdgeSuggestion> list = suggestionRepository
                .findBySourceNodeIdAndStatus(nodeId, AiSuggestionStatus.PENDING);
        return Result.success(toDtos(list));
    }

    /** 接受建议 */
    @PostMapping("/{id}/accept")
    @Transactional
    public Result<NodeEdge> accept(@PathVariable Long id) {
        AiEdgeSuggestion sug = requireOwnSuggestion(id);
        if (sug.getStatus() != AiSuggestionStatus.PENDING) {
            throw new RuntimeException("建议不存在");
        }
        sug.setStatus(AiSuggestionStatus.ACCEPTED);
        sug.setDecidedAt(LocalDateTime.now());
        suggestionRepository.save(sug);

        NodeEdge edge = new NodeEdge();
        edge.setSourceNodeId(sug.getSourceNodeId());
        edge.setTargetNodeId(sug.getTargetNodeId());
        edge.setEdgeType(EdgeType.AI_SUGGESTED);
        edge.setEdgeStatus(EdgeStatus.ACCEPTED);
        edge.setAcceptedAt(LocalDateTime.now());
        edge.setSourceText(sug.getSuggestionReason());
        return Result.success(edgeRepository.save(edge));
    }

    /** 拒绝建议 — 强化学习关键:存下来,以后不再推 */
    @PostMapping("/{id}/reject")
    @Transactional
    public Result<AiEdgeSuggestion> reject(@PathVariable Long id) {
        AiEdgeSuggestion sug = requireOwnSuggestion(id);
        sug.setStatus(AiSuggestionStatus.REJECTED);
        sug.setDecidedAt(LocalDateTime.now());
        return Result.success(suggestionRepository.save(sug));
    }

    private AiEdgeSuggestion requireOwnSuggestion(Long id) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) {
            throw new RuntimeException("建议不存在");
        }
        AiEdgeSuggestion sug = suggestionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("建议不存在"));
        if (sug.getUserId() == null || !sug.getUserId().equals(userId)) {
            throw new RuntimeException("建议不存在");
        }
        return sug;
    }

    private List<AiSuggestionWithTitle> toDtos(List<AiEdgeSuggestion> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        Set<Long> ids = new HashSet<>();
        for (AiEdgeSuggestion s : list) {
            ids.add(s.getSourceNodeId());
            ids.add(s.getTargetNodeId());
        }
        Map<Long, String> titles = edgeMapper.titlesByIds(ids);
        List<AiSuggestionWithTitle> result = new ArrayList<>();
        for (AiEdgeSuggestion s : list) {
            AiSuggestionWithTitle dto = new AiSuggestionWithTitle();
            dto.setId(s.getId());
            dto.setSourceNodeId(s.getSourceNodeId());
            dto.setSourceNodeTitle(titles.getOrDefault(s.getSourceNodeId(), "(已删除)"));
            dto.setTargetNodeId(s.getTargetNodeId());
            dto.setTargetNodeTitle(titles.getOrDefault(s.getTargetNodeId(), "(已删除)"));
            dto.setScore(s.getScore());
            dto.setSuggestionReason(s.getSuggestionReason());
            dto.setStatus(s.getStatus());
            dto.setCreatedAt(s.getCreatedAt());
            result.add(dto);
        }
        return result;
    }
}
