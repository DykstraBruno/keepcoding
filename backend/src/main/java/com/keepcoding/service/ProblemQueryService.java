package com.keepcoding.service;

import com.keepcoding.domain.Problem;
import com.keepcoding.dto.ProblemDetailResponse;
import com.keepcoding.dto.ProblemSummary;
import com.keepcoding.dto.TestCaseView;
import com.keepcoding.repository.ProblemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** Consultas de leitura sobre problemas. */
@Service
@RequiredArgsConstructor
public class ProblemQueryService {

    private final ProblemRepository problemRepository;

    /** Lista todos os problemas (resumo), ordenados por dificuldade e título. */
    @Transactional(readOnly = true)
    public List<ProblemSummary> listAll() {
        return problemRepository.findAll().stream()
                .sorted(Comparator.comparing(Problem::getDifficulty).thenComparing(Problem::getTitle))
                .map(p -> new ProblemSummary(p.getId(), p.getTitle(), p.getDifficulty()))
                .toList();
    }

    /** Detalhe de um problema; expõe apenas os casos de teste de exemplo. */
    @Transactional(readOnly = true)
    public ProblemDetailResponse getById(Long id) {
        Problem p = problemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Problema não encontrado: " + id));

        List<TestCaseView> samples = p.getTestCases().stream()
                .filter(tc -> Boolean.TRUE.equals(tc.getIsSample()))
                .map(tc -> new TestCaseView(tc.getId(), tc.getInput(),
                        tc.getExpectedOutput(), tc.getIsSample()))
                .toList();

        return new ProblemDetailResponse(
                p.getId(), p.getTitle(), p.getDescription(), p.getDifficulty(),
                p.getTimeLimit(), p.getMemoryLimit(), samples);
    }
}
