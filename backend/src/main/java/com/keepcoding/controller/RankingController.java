package com.keepcoding.controller;

import com.keepcoding.dto.RankingResponse;
import com.keepcoding.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ranking global por XP, com contagem de resolvidos por dificuldade. */
@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private static final int TOP_LIMIT = 50;

    private final RankingService rankingService;

    @GetMapping
    public RankingResponse get(@AuthenticationPrincipal UserDetails principal) {
        return rankingService.compute(TOP_LIMIT, principal.getUsername());
    }
}
