package com.LinkedKnowledge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** 思维导图节点 DTO：树形 id/topic/children，可带自由坐标与入边文字 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MindMapNode {
    private String id;
    private String topic;
    private Double x;
    private Double y;
    /** 从父节点连到本节点的边上文字 */
    private String linkLabel;
    private List<MindMapNode> children = new ArrayList<>();

    public MindMapNode(String id, String topic) {
        this.id = id;
        this.topic = topic;
    }
}