package com.keepcoding.controller;

import com.keepcoding.dto.ProblemDetailResponse;
import com.keepcoding.dto.ProblemSummary;
import com.keepcoding.service.ProblemQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Endpoints de leitura de problemas. */
@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemQueryService problemQueryService;

    /** Lista todos os problemas (resumo) para o dashboard, marcando os já resolvidos pelo usuário. */
    @GetMapping
    public List<ProblemSummary> list(@AuthenticationPrincipal UserDetails principal) {
        return problemQueryService.listAll(principal.getUsername());
    }

    /** Detalhe de um problema. */
    @GetMapping("/{id}")
    public ProblemDetailResponse get(@PathVariable Long id) {
        return problemQueryService.getById(id);
    }
}
