package com.keepcoding.service;

import com.keepcoding.domain.Problem;
import com.keepcoding.domain.User;
import com.keepcoding.dto.ProblemDetailResponse;
import com.keepcoding.dto.ProblemSummary;
import com.keepcoding.dto.TestCaseView;
import com.keepcoding.repository.ProblemRepository;
import com.keepcoding.repository.SolvedProblemRepository;
import com.keepcoding.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Consultas de leitura sobre problemas. */
@Service
@RequiredArgsConstructor
public class ProblemQueryService {

    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final SolvedProblemRepository solvedProblemRepository;

    /**
     * Lista todos os problemas (resumo) marcando quais já foram resolvidos
     * pelo usuário autenticado.
     */
    @Transactional(readOnly = true)
    public List<ProblemSummary> listAll(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + userEmail));
        Set<Long> solvedIds = solvedProblemRepository.findSolvedProblemIdsByUserId(user.getId());

        return problemRepository.findAll().stream()
                .sorted(Comparator.comparing(Problem::getDifficulty).thenComparing(Problem::getTitle))
                .map(p -> new ProblemSummary(
                        p.getId(),
                        p.getTitle(),
                        p.getDifficulty(),
                        solvedIds.contains(p.getId())))
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
