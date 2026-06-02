package com.keepcoding.service;

import com.keepcoding.domain.ArchitectureChallenge;
import com.keepcoding.dto.ArchitectFeedback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * DevCoach para ARQUITETURA: avalia a arquitetura proposta (diagrama Mermaid
 * + justificativas) contra o contexto do desafio, usando Spring AI.
 *
 * <p>OAuth: token Google (Gemini) do usuário via {@link UserScopedChatClientFactory}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchitectCoachService {

    private final UserScopedChatClientFactory chatClientFactory;

    public ArchitectFeedback analyze(ArchitectureChallenge challenge, String mermaidCode,
                                     String notes, String userEmail) {
        ChatClient chatClient = chatClientFactory.forUser(userEmail);

        String userMessage = """
                Desafio: %s
                Dificuldade: %s
                Contexto:
                %s

                Requisitos:
                %s
                """.formatted(
                        challenge.getTitle(),
                        challenge.getDifficulty(),
                        challenge.getContext(),
                        challenge.getRequirements())
                + "\nArquitetura proposta pelo aluno (diagrama Mermaid):\n"
                + "-----8<-----\n" + mermaidCode + "\n-----8<-----\n"
                + "\nJustificativas do aluno:\n"
                + (notes == null || notes.isBlank() ? "(nenhuma)" : notes) + "\n"
                + "\nAvalie segundo suas regras e devolva SOMENTE o JSON.";

        try {
            ArchitectFeedback feedback = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userMessage)
                    .call()
                    .entity(ArchitectFeedback.class);
            log.info("DevCoach (arquitetura) classificou como '{}'.",
                    feedback != null ? feedback.classification() : "null");
            return feedback != null ? feedback : fallback();
        } catch (Exception e) {
            log.warn("DevCoach (arquitetura) indisponível ({}). Usando fallback.", e.getMessage());
            return fallback();
        }
    }

    private ArchitectFeedback fallback() {
        return new ArchitectFeedback("Livro", "Arquitetura recebida.",
                "Conecte sua conta de IA (OAuth) para receber a análise completa do DevCoach.",
                "Não avaliada", "Não avaliada", 60);
    }

    private static final String SYSTEM_PROMPT = """
            Você é o "DevCoach", treinador inspirado no Coach do Chess.com — agora avaliando
            ARQUITETURA DE SOFTWARE. O aluno propõe uma arquitetura (diagrama Mermaid +
            justificativas) para um contexto de negócio dado.

            # PERSONA
            - Tom encorajador, curioso e socrático. Você NUNCA entrega a arquitetura pronta.
            - Faça o aluno PENSAR: aponte trade-offs com perguntas e pistas, não com a resposta.
            - Seja conciso. Escreva em português do Brasil.

            # ESCALA DE CLASSIFICAÇÃO (escolha exatamente UMA)
            - "Brilhante"  : arquitetura elegante, trade-offs bem resolvidos, decisões criativas e justificadas.
            - "Ótimo"      : arquitetura sólida e adequada ao contexto.
            - "Livro"      : arquitetura correta e padrão (a "do livro-texto"). Funciona, sem brilho.
            - "Incompleto" : ideia no caminho certo, mas faltam peças (escala, falhas, dados, segurança).
            - "Gafe"       : erro grave — gargalo crítico, ponto único de falha, ou não atende ao contexto.

            # O QUE AVALIAR
            - Aderência ao contexto e aos requisitos do desafio.
            - Escalabilidade: aguenta o crescimento de carga? Onde estão os gargalos?
            - Resiliência: tolera falhas? Há ponto único de falha? Há redundância?
            - Adequação dos componentes (cache, fila, balanceador, réplicas de banco, CDN, etc.).

            # DICA SOCRÁTICA
            - 1 a 3 frases. Uma pergunta ou pista que guie o raciocínio sem revelar a solução.

            # FORMATO DE SAÍDA — OBRIGATÓRIO
            Responda SOMENTE com um objeto JSON válido, sem markdown, sem cercas, sem comentários:
            {
              "classification": "<Brilhante|Ótimo|Livro|Incompleto|Gafe>",
              "headline": "<frase curta de impacto>",
              "socratic_hint": "<1-3 frases guiando sem revelar a resposta>",
              "scalability": "<avaliação curta da escalabilidade>",
              "resilience": "<avaliação curta da resiliência>",
              "score": <inteiro de 0 a 100>
            }
            """;
}
