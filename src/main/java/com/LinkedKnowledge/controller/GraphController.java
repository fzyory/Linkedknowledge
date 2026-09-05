package com.LinkedKnowledge.controller;

import com.LinkedKnowledge.common.AuthHelper;
import com.LinkedKnowledge.common.Result;
import com.LinkedKnowledge.dto.GraphData;
import com.LinkedKnowledge.entity.EdgeStatus;
import com.LinkedKnowledge.entity.KnowledgeNode;
import com.LinkedKnowledge.entity.NodeEdge;
import com.LinkedKnowledge.repository.KnowledgeNodeRepository;
import com.LinkedKnowledge.repository.NodeEdgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
public class GraphController {

    private static final int MAX_NODES = 500;

    private final KnowledgeNodeRepository nodeRepository;
    private final NodeEdgeRepository edgeRepository;

    /**
     * 返回当前用户的所有节点 + 所有 ACCEPTED 边
     * 用于前端图谱可视化
     */
    @GetMapping
    public Result<GraphData> getMyGraph() {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        List<KnowledgeNode> nodes = nodeRepository.findByUserId(userId);
        if (nodes.size() > MAX_NODES) {
            nodes = new ArrayList<>(nodes.subList(0, MAX_NODES));
        }
        Set<Long> nodeIds = nodes.stream().map(KnowledgeNode::getId).collect(Collectors.toSet());

        List<NodeEdge> edges;
        if (nodeIds.isEmpty()) {
            edges = List.of();
        } else {
            edges = edgeRepository.findByEdgeStatusAndSourceNodeIdIn(EdgeStatus.ACCEPTED, nodeIds)
                    .stream()
                    .filter(e -> nodeIds.contains(e.getSourceNodeId()) && nodeIds.contains(e.getTargetNodeId()))
                    .distinct()
                    .collect(Collectors.toList());
        }

        GraphData data = new GraphData();
        data.setNodes(nodes.stream().map(n -> {
            GraphData.Node gn = new GraphData.Node();
            gn.setId(n.getId());
            gn.setTitle(n.getTitle());
            gn.setType(n.getType() == null ? null : n.getType().name());
            return gn;
        }).collect(Collectors.toList()));
        data.setEdges(edges.stream().map(e -> {
            GraphData.Edge ge = new GraphData.Edge();
            ge.setSource(e.getSourceNodeId());
            ge.setTarget(e.getTargetNodeId());
            ge.setType(e.getEdgeType() == null ? null : e.getEdgeType().name());
            return ge;
        }).collect(Collectors.toList()));
        return Result.success(data);
    }
}
