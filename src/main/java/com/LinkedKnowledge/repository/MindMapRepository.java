package com.LinkedKnowledge.repository;

import com.LinkedKnowledge.entity.MindMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MindMapRepository extends JpaRepository<MindMap, Long> {

    /** 查询某用户的所有导图（按更新时间倒序） */
    List<MindMap> findByUserIdOrderByUpdatedAtDesc(Long userId);

    /** 按标题模糊搜索（某用户范围内） */
    List<MindMap> findByUserIdAndTitleContainingOrderByUpdatedAtDesc(Long userId, String keyword);
}