package com.LinkedKnowledge.repository;

import com.LinkedKnowledge.entity.EdgeStatus;
import com.LinkedKnowledge.entity.EdgeType;
import com.LinkedKnowledge.entity.NodeEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface NodeEdgeRepository extends JpaRepository<NodeEdge, Long> {

    List<NodeEdge> findByTargetNodeIdAndEdgeStatus(Long targetNodeId, EdgeStatus edgeStatus);

    List<NodeEdge> findBySourceNodeIdAndEdgeStatus(Long sourceNodeId, EdgeStatus edgeStatus);

    List<NodeEdge> findByEdgeStatusAndEdgeType(EdgeStatus edgeStatus, EdgeType edgeType);

    default List<NodeEdge> findAcceptedByTarget(Long nodeId) {
        return findByTargetNodeIdAndEdgeStatus(nodeId, EdgeStatus.ACCEPTED);
    }

    default List<NodeEdge> findAcceptedBySource(Long nodeId) {
        return findBySourceNodeIdAndEdgeStatus(nodeId, EdgeStatus.ACCEPTED);
    }

    default List<NodeEdge> findPendingAiEdges() {
        return findByEdgeStatusAndEdgeType(EdgeStatus.PENDING, EdgeType.AI_SUGGESTED);
    }

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM NodeEdge e WHERE e.sourceNodeId = :nodeId AND e.edgeType = :edgeType")
    void deleteBySourceNodeIdAndEdgeType(@Param("nodeId") Long nodeId, @Param("edgeType") EdgeType edgeType);

    default void deleteParsedLinksBySource(Long nodeId) {
        deleteBySourceNodeIdAndEdgeType(nodeId, EdgeType.PARSED_LINK);
    }

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM NodeEdge e WHERE e.sourceNodeId = :nodeId OR e.targetNodeId = :nodeId")
    void deleteAllByNodeId(@Param("nodeId") Long nodeId);

    List<NodeEdge> findByEdgeStatus(EdgeStatus status);

    List<NodeEdge> findByEdgeStatusAndSourceNodeIdIn(EdgeStatus status, Collection<Long> ids);
}
