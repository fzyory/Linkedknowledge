package com.LinkedKnowledge.repository;

import com.LinkedKnowledge.entity.VaultSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VaultSettingsRepository extends JpaRepository<VaultSettings, Long> {
    Optional<VaultSettings> findByUserId(Long userId);
}
