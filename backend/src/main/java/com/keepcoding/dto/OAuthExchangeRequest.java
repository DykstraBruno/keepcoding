package com.keepcoding.dto;

import jakarta.validation.constraints.NotBlank;

/** Payload de POST /api/oauth/{provider}/exchange (chamado pelo callback do front). */
public record OAuthExchangeRequest(
        @NotBlank String code,
        @NotBlank String state
) {}
