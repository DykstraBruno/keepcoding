package com.keepcoding.service;

import com.keepcoding.domain.Problem;
import com.keepcoding.domain.enums.Language;
import com.keepcoding.domain.enums.SubmissionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Camada de sandbox / execucao de codigo.
 *
 * <p>Hoje SIMULA a execucao para permitir testar o fluxo ponta-a-ponta
 * sem credenciais. Para integrar o Judge0 real, basta substituir o corpo
 * de {@link #execute} pela chamada HTTP descrita no bloco de comentario.</p>
 *
 * <p>IDs de linguagem no Judge0 CE: Java = 62 | TypeScript = 74.</p>
 */
@Slf4j
@Service
public class Judge0Service {

    @Value("${judge0.api-url}")
    private String apiUrl;

    @Value("${judge0.api-key:}")
    private String apiKey;

    /** Resultado consolidado da execucao contra todos os casos de teste. */
    public record ExecutionResult(
            SubmissionStatus status,
            int passedTests,
            int totalTests,
            String stdout,
            String stderr
    ) {}

    /**
     * Executa o codigo do usuario contra os casos de teste do problema.
     *
     * @param problem  problema com seus casos de teste carregados
     * @param language linguagem do codigo
     * @param code     codigo-fonte submetido
     */
    public ExecutionResult execute(Problem problem, Language language, String code) {
        log.info("[Judge0-MOCK] Executando | problema='{}' | linguagem={}",
                problem.getTitle(), language);

        int total = problem.getTestCases().size();

        // ----------------------------- SIMULACAO -----------------------------
        // Sem Judge0 real, aplicamos heurísticas simples para evitar que
        // qualquer código vazio/trivial seja marcado como ACCEPTED.
        // Substituir tudo isso pela chamada real ao Judge0 quando integrado.
        if (code == null || code.isBlank()) {
            return new ExecutionResult(SubmissionStatus.ERROR, 0, total, "", "Codigo vazio");
        }
        String trimmed = code.trim();
        if (trimmed.length() < 40) {
            return new ExecutionResult(SubmissionStatus.WRONG_ANSWER, 0, total,
                    "", "Solucao curta demais para ser uma resposta (mock).");
        }
        boolean hasLogicMarker =
                   trimmed.contains("return")
                || trimmed.contains("def ")
                || trimmed.contains("function")
                || trimmed.contains("fn ")
                || trimmed.contains("void ")
                || trimmed.contains("public ")
                || trimmed.contains("=>")
                || trimmed.contains("->")
                || trimmed.contains("for ")
                || trimmed.contains("while ")
                || trimmed.contains("if ");
        if (!hasLogicMarker) {
            return new ExecutionResult(SubmissionStatus.WRONG_ANSWER, 0, total,
                    "", "Codigo nao parece ter logica de solucao (mock).");
        }
        return new ExecutionResult(
                SubmissionStatus.ACCEPTED, total, total,
                "Todos os casos de teste passaram (mock).", "");
        // --------------------------- FIM SIMULACAO ----------------------------

        /* ===================================================================
         * INTEGRACAO REAL COM JUDGE0 (descomentar e ajustar):
         *
         *   var languageId = switch (language) {
         *       case JAVA -> 62;
         *       case TYPESCRIPT -> 74;
         *   };
         *   RestClient client = RestClient.builder().baseUrl(apiUrl).build();
         *   for (TestCase tc : problem.getTestCases()) {
         *       var body = Map.of(
         *           "source_code", code,
         *           "language_id", languageId,
         *           "stdin", tc.getInput(),
         *           "expected_output", tc.getExpectedOutput(),
         *           "cpu_time_limit", problem.getTimeLimit() / 1000.0);
         *       var resp = client.post()
         *           .uri("/submissions?wait=true&base64_encoded=false")
         *           .header("X-RapidAPI-Key", apiKey)
         *           .body(body)
         *           .retrieve()
         *           .body(Judge0Response.class);
         *       // mapear resp.status().id() -> SubmissionStatus
         *   }
         * =================================================================== */
    }
}
