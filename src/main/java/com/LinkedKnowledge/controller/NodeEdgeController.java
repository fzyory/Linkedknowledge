package com.LinkedKnowledge.controller;

import com.LinkedKnowledge.common.AuthHelper;
import com.LinkedKnowledge.common.Result;
import com.LinkedKnowledge.dto.EdgeCreateRequest;
import com.LinkedKnowledge.dto.NodeEdgeWithTitle;
import com.LinkedKnowledge.entity.EdgeStatus;
import com.LinkedKnowledge.entity.EdgeType;
import com.LinkedKnowledge.entity.NodeEdge;
import com.LinkedKnowledge.repository.NodeEdgeRepository;
import com.LinkedKnowledge.service.KnowledgeNodeService;
import com.LinkedKnowledge.service.NodeEdgeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/edges")
@RequiredArgsConstructor
public class NodeEdgeController {

    private final NodeEdgeRepository edgeRepository;
    private final NodeEdgeMapper edgeMapper;
    private final KnowledgeNodeService nodeService;

    /** 建边 */
    @PostMapping
    public Result<NodeEdge> create(@RequestBody EdgeCreateRequest req) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        nodeService.assertOwned(req.getSourceNodeId(), userId);
        nodeService.assertOwned(req.getTargetNodeId(), userId);
        NodeEdge edge = new NodeEdge();
        edge.setSourceNodeId(req.getSourceNodeId());
        edge.setTargetNodeId(req.getTargetNodeId());
        edge.setEdgeType(req.getEdgeType());
        edge.setSourceText(req.getSourceText());

        if (req.getEdgeType() == EdgeType.AI_SUGGESTED) {
            edge.setEdgeStatus(EdgeStatus.PENDING);
        } else {
            edge.setEdgeStatus(EdgeStatus.ACCEPTED);
            edge.setAcceptedAt(LocalDateTime.now());
        }

        return Result.success(edgeRepository.save(edge));
    }

    /** 删边 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        NodeEdge edge = edgeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("节点不存在"));
        nodeService.assertOwned(edge.getSourceNodeId(), userId);
        edgeRepository.deleteById(id);
        return Result.success();
    }

    /** 正向链接 — 节点 X 引用了谁 */
    @GetMapping("/source/{nodeId}")
    public Result<List<NodeEdgeWithTitle>> bySource(@PathVariable Long nodeId) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        nodeService.assertOwned(nodeId, userId);
        return Result.success(edgeMapper.toWithTitle(edgeRepository.findAcceptedBySource(nodeId)));
    }

    /** 反向链接 — 谁引用了节点 X */
    @GetMapping("/target/{nodeId}")
    public Result<List<NodeEdgeWithTitle>> byTarget(@PathVariable Long nodeId) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        nodeService.assertOwned(nodeId, userId);
        return Result.success(edgeMapper.toWithTitle(edgeRepository.findAcceptedByTarget(nodeId)));
    }
}
