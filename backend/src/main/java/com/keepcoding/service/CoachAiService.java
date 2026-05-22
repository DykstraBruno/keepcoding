package com.keepcoding.service;

import com.keepcoding.domain.Problem;
import com.keepcoding.domain.enums.Language;
import com.keepcoding.dto.CoachFeedback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * DevCoach: avalia a QUALIDADE de uma solucao usando Spring AI.
 *
 * <p>Inspirado no "Coach" do Chess.com: em vez de so dizer passou/falhou,
 * classifica a solucao (Brilhante, Otimo, Livro, Incompleto, Gafe), da uma
 * dica socratica e calcula a complexidade Big O.</p>
 *
 * <p>Se a API de IA estiver indisponivel (ex.: sem OPENAI_API_KEY), retorna
 * um feedback de fallback para o fluxo nao quebrar em desenvolvimento.</p>
 */
@Slf4j
@Service
public class CoachAiService {

    private final ChatClient chatClient;

    public CoachAiService(ChatClient.Builder builder) {
        this.chatClient = builder.defaultSystem(SYSTEM_PROMPT).build();
    }

    /**
     * Analisa o codigo submetido e produz o feedback do DevCoach.
     *
     * @param accepted true se a submissao passou em todos os testes
     */
    public CoachFeedback analyze(Problem problem, Language language, String code, boolean accepted) {
        String verdict = accepted
                ? "ACEITO - passou em todos os casos de teste"
                : "REPROVADO - falhou em um ou mais casos de teste";

        // O codigo do aluno e injetado por concatenacao (NUNCA via template):
        // chaves { } presentes em Java/TS quebrariam o template engine do Spring AI.
        String userMessage = """
                Problema: %s
                Dificuldade: %s
                Descricao do problema:
                %s

                Linguagem: %s
                Resultado da execucao no sandbox: %s
                """.formatted(
                        problem.getTitle(),
                        problem.getDifficulty(),
                        problem.getDescription(),
                        language,
                        verdict)
                + "\nCodigo submetido pelo aluno:\n"
                + "-----8<-----\n" + code + "\n-----8<-----\n"
                + "\nAvalie segundo suas regras e devolva SOMENTE o JSON.";

        try {
            CoachFeedback feedback = chatClient.prompt()
                    .user(userMessage)
                    .call()
                    .entity(CoachFeedback.class);
            log.info("DevCoach classificou a submissao como '{}'.",
                    feedback != null ? feedback.classification() : "null");
            return feedback != null ? feedback : fallback(accepted);
        } catch (Exception e) {
            log.warn("DevCoach indisponivel ({}). Usando feedback de fallback.", e.getMessage());
            return fallback(accepted);
        }
    }

    /** Feedback de contingencia quando a IA nao responde. */
    private CoachFeedback fallback(boolean accepted) {
        return accepted
                ? new CoachFeedback("Livro", "Solucao aceita!",
                    "Configure OPENAI_API_KEY para receber a analise completa do DevCoach.",
                    "O(?)", "O(?)", 70)
                : new CoachFeedback("Incompleto", "Ainda nao chegou la.",
                    "Revise os casos de borda. Configure OPENAI_API_KEY para dicas detalhadas.",
                    "O(?)", "O(?)", 30);
    }

    // ------------------------------------------------------------------------
    // System Prompt: define a persona, a escala de classificacao e o formato.
    // ------------------------------------------------------------------------
    private static final String SYSTEM_PROMPT = """
            Voce e o "DevCoach", um treinador de programacao inspirado no Coach do Chess.com.
            Sua missao e avaliar a QUALIDADE de uma solucao de algoritmo - nao apenas se ela passou.

            # PERSONA
            - Tom encorajador, curioso e socratico. Voce NUNCA entrega a solucao pronta.
            - Faca o aluno PENSAR: aponte a direcao com perguntas e pistas, jamais com o codigo da resposta.
            - Seja conciso e direto, como um mentor que respeita o tempo do aluno.
            - Escreva sempre em portugues do Brasil.

            # ESCALA DE CLASSIFICACAO (escolha exatamente UMA)
            - "Brilhante"  : solucao otima, elegante e engenhosa. Complexidade ideal e abordagem criativa.
            - "Otimo"      : solucao correta e eficiente, dentro do esperado para o problema.
            - "Livro"      : solucao correta e padrao - a abordagem classica "do livro-texto". Solida, sem brilho.
            - "Incompleto" : a ideia esta no caminho certo, mas falha em casos de borda ou na eficiencia.
            - "Gafe"       : erro conceitual grave, solucao incorreta ou de complexidade proibitiva.

            # ANALISE DE COMPLEXIDADE (Big O)
            - Sempre calcule a complexidade de TEMPO e de ESPACO da solucao enviada.
            - Use notacao padrao: O(1), O(log n), O(n), O(n log n), O(n^2), O(2^n).

            # DICA SOCRATICA
            - 1 a 3 frases. Uma pergunta ou pista que guie o raciocinio.
            - Se a solucao foi "Brilhante" ou "Otimo": parabenize e proponha um proximo desafio mental.
            - Se foi "Incompleto" ou "Gafe": aponte ONDE pensar, sem dizer O QUE escrever.

            # FORMATO DE SAIDA - OBRIGATORIO
            Responda SOMENTE com um objeto JSON valido, sem markdown, sem cercas de codigo, sem comentarios:
            {
              "classification": "<Brilhante|Otimo|Livro|Incompleto|Gafe>",
              "headline": "<frase curta de impacto>",
              "socratic_hint": "<1-3 frases guiando sem revelar a resposta>",
              "time_complexity": "<ex: O(n)>",
              "space_complexity": "<ex: O(1)>",
              "score": <inteiro de 0 a 100>
            }
            """;
}
