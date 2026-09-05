package com.LinkedKnowledge.entity;

public enum EdgeType {
    MANUAL_BUTTON,    // 用户在 UI 上点"连接"按钮建的
    PARSED_LINK,      // 系统从 [[xxx]] 语法解析出来的
    AI_SUGGESTED      // AI 算出来的(此时 edge_status 一开始是 PENDING)
}
