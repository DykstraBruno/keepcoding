package com.keepcoding.dto;

import com.keepcoding.domain.enums.MessageRole;

import java.time.Instant;

/** Uma mensagem do diálogo no formato exposto ao frontend. */
public record InterviewMessageView(
        MessageRole role,
        Integer turnIndex,
        String content,
        Instant createdAt
) {}
