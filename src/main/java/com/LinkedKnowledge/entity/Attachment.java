package com.LinkedKnowledge.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "vault_attachment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long nodeId;
    @Column(length = 300)
    private String filename;
    @Column(length = 120)
    private String contentType;
    @JsonIgnore
    @Column(columnDefinition = "LONGBLOB")
    private byte[] data;
    private LocalDateTime createdAt = LocalDateTime.now();
}
