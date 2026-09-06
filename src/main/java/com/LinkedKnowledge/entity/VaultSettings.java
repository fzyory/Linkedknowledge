package com.LinkedKnowledge.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vault_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VaultSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private Long userId;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String settingsJson = "{}";
    @Column(columnDefinition = "MEDIUMTEXT")
    private String workspaceJson = "{}";
}
