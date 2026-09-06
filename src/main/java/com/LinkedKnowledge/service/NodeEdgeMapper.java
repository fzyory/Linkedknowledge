package com.LinkedKnowledge.service;

import com.LinkedKnowledge.dto.NodeEdgeWithTitle;
import com.LinkedKnowledge.entity.KnowledgeNode;
import com.LinkedKnowledge.entity.NodeEdge;
import com.LinkedKnowledge.repository.KnowledgeNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class NodeEdgeMapper {

    private static final String DELETED_TITLE = "(已删除)";

    private final KnowledgeNodeRepository nodeRepository;

    public Map<Long, String> titlesByIds(Collection<Long> ids) {
        Map<Long, String> titles = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return titles;
        }
        for (KnowledgeNode n : nodeRepository.findAllById(ids)) {
            titles.put(n.getId(), n.getTitle() == null ? DELETED_TITLE : n.getTitle());
        }
        return titles;
    }

    public List<NodeEdgeWithTitle> toWithTitle(List<NodeEdge> edges) {
        if (edges == null || edges.isEmpty()) {
            return List.of();
        }
        Set<Long> ids = new HashSet<>();
        for (NodeEdge e : edges) {
            ids.add(e.getSourceNodeId());
            ids.add(e.getTargetNodeId());
        }
        Map<Long, String> titles = titlesByIds(ids);
        List<NodeEdgeWithTitle> result = new ArrayList<>();
        for (NodeEdge e : edges) {
            NodeEdgeWithTitle dto = new NodeEdgeWithTitle();
            dto.setEdgeId(e.getId());
            dto.setSourceNodeId(e.getSourceNodeId());
            dto.setSourceNodeTitle(titles.getOrDefault(e.getSourceNodeId(), DELETED_TITLE));
            dto.setTargetNodeId(e.getTargetNodeId());
            dto.setTargetNodeTitle(titles.getOrDefault(e.getTargetNodeId(), DELETED_TITLE));
            dto.setEdgeType(e.getEdgeType());
            dto.setCreatedAt(e.getCreatedAt());
            result.add(dto);
        }
        return result;
    }
}
