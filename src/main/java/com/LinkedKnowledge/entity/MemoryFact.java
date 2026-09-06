package com.LinkedKnowledge.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "memory_fact")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoryFact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long userId;
    private Long nodeId;
    @Column(length = 20, nullable = false)
    private String kind = "NOTE";
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    private LocalDateTime createdAt = LocalDateTime.now();
}
