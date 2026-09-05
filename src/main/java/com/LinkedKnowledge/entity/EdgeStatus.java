package com.LinkedKnowledge.entity;

public enum EdgeStatus {
    PENDING,    // 待用户决定(只对 AI_SUGGESTED 有意义)
    ACCEPTED,   // 已接受
    REJECTED    // 已拒绝
}
