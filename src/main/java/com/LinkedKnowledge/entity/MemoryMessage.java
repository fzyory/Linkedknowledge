package com.LinkedKnowledge.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "memory_message")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoryMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long userId;
    @Column(length = 20, nullable = false)
    private String role;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    private Long relatedNodeId;
    private LocalDateTime createdAt = LocalDateTime.now();
}
