package com.LinkedKnowledge.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "vault_bookmark")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bookmark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long nodeId;
    @Column(length = 200)
    private String title;
    @Column(length = 200)
    private String groupName = "默认";
    private LocalDateTime createdAt = LocalDateTime.now();
}
