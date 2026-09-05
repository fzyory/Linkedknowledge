package com.LinkedKnowledge.repository;

import com.LinkedKnowledge.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    @Query("SELECT DISTINCT t FROM Tag t JOIN t.nodes n WHERE n.userId = :userId")
    List<Tag> findUsedByUserId(@Param("userId") Long userId);
}
