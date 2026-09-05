package com.LinkedKnowledge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoryChatResponse {
    private String reply;
    private List<NodeSummary> sources = new ArrayList<>();
}
