package com.LinkedKnowledge.entity;

public enum AiSuggestionStatus {
    PENDING,    // 待用户决定
    ACCEPTED,   // 已接受 → 已建 NodeEdge
    REJECTED,   // 已拒绝
    EXPIRED     // 7 天未处理 → 自动失效(后台定时任务跑)
}
