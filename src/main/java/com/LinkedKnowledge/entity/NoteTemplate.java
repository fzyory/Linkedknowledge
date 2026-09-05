package com.LinkedKnowledge.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "vault_template")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String content = "";
    private LocalDateTime createdAt = LocalDateTime.now();
}
