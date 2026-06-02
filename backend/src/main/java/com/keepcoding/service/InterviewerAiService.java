package com.keepcoding.service;

import com.keepcoding.domain.Interview;
import com.keepcoding.domain.InterviewMessage;
import com.keepcoding.domain.enums.MessageRole;
import com.keepcoding.dto.InterviewFeedback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * IA-entrevistadora: faz perguntas personalizadas com base no currículo +
 * vaga, mantém coerência ao longo dos turnos e produz um feedback final
 * estruturado quando a entrevista encerra.
 *
 * <p>OAuth: usa token Google (Gemini) do usuário via {@link UserScopedChatClientFactory}.
 * O contexto (currículo, vaga, histórico) é injetado no system prompt — invisível
 * ao candidato, que só vê a conversa natural do entrevistador.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewerAiService {

    private final UserScopedChatClientFactory chatClientFactory;

    /** Produz a próxima pergunta do entrevistador, considerando todo o histórico. */
    public String nextQuestion(Interview interview, List<InterviewMessage> history, String userEmail) {
        ChatClient chatClient = chatClientFactory.forUser(userEmail);

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPromptForInterview(interview)));
        for (InterviewMessage m : history) {
            messages.add(m.getRole() == MessageRole.INTERVIEWER
                    ? new AssistantMessage(m.getContent())
                    : new UserMessage(m.getContent()));
        }
        if (history.isEmpty()) {
            // Bootstrap: instrução do usuário para o modelo abrir a entrevista
            // com a fase de APRESENTAÇÃO (cumprimento + pedido de auto-apresentação).
            messages.add(new UserMessage(
                    "[INICIAR ENTREVISTA] Comece pela fase de APRESENTAÇÃO: cumprimente "
                    + "o candidato e peça que ele se apresente em até 10 minutos."));
        }
        try {
            String response = chatClient.prompt(new Prompt(messages))
                    .call()
                    .content();
            return (response == null || response.isBlank())
                    ? fallbackQuestion(history.size() / 2 + 1)
                    : response.trim();
        } catch (Exception e) {
            log.warn("InterviewerAiService.nextQuestion indisponível ({}). Fallback.", e.getMessage());
            return fallbackQuestion(history.size() / 2 + 1);
        }
    }

    /** Gera o feedback final estruturado a partir da transcrição completa. */
    public InterviewFeedback produceFeedback(Interview interview, List<InterviewMessage> history, String userEmail) {
        ChatClient chatClient = chatClientFactory.forUser(userEmail);

        StringBuilder transcript = new StringBuilder();
        for (InterviewMessage m : history) {
            transcript.append(m.getRole() == MessageRole.INTERVIEWER ? "ENTREVISTADOR" : "CANDIDATO")
                    .append(" (turno ").append(m.getTurnIndex()).append("):\n")
                    .append(m.getContent()).append("\n\n");
        }

        List<Message> messages = List.of(
                new SystemMessage(systemPromptForFeedback(interview)),
                new UserMessage("Transcrição da entrevista:\n\n" + transcript
                        + "\nAvalie segundo as regras e devolva SOMENTE o JSON.")
        );

        try {
            InterviewFeedback feedback = chatClient.prompt(new Prompt(messages))
                    .call()
                    .entity(InterviewFeedback.class);
            return feedback != null ? feedback : fallbackFeedback();
        } catch (Exception e) {
            log.warn("InterviewerAiService.produceFeedback indisponível ({}). Fallback.", e.getMessage());
            return fallbackFeedback();
        }
    }

    // ---------------------------------------------------------------- prompts
    private String systemPromptForInterview(Interview i) {
        return """
                Você é um entrevistador profissional sênior do KeepCoding, conduzindo
                uma entrevista no FORMATO PADRÃO DA INDÚSTRIA DE TECNOLOGIA:

                DURAÇÃO TOTAL: 40 minutos
                - 10 min de APRESENTAÇÃO (candidato fala sobre si).
                - 30 min de BLOCO DE PERGUNTAS (aproximadamente 4 min por par
                  pergunta+resposta → %d perguntas no total).

                Vaga-alvo: %s

                Currículo do candidato:
                ---
                %s
                ---

                ESTRUTURA OBRIGATÓRIA DAS SUAS MENSAGENS:
                - 1ª mensagem (turno 0): cumprimente o candidato com cordialidade,
                  mencione brevemente a vaga e PEÇA EXPLICITAMENTE QUE ELE SE
                  APRESENTE em até 10 minutos (trajetória, principais experiências,
                  motivação para a vaga). NÃO faça nenhuma pergunta técnica aqui.
                - Da 2ª mensagem em diante: faça UMA pergunta técnica ou
                  comportamental por turno, personalizada ao currículo E à
                  apresentação que o candidato deu. Cada pergunta deve caber em
                  ~4 minutos de resposta — direta, focada em UM tópico, NUNCA
                  composta (não pergunte 3 coisas de uma vez).

                REGRAS GERAIS:
                - Cada resposta sua = APENAS a próxima mensagem do entrevistador.
                  Sem preâmbulo, sem comentário sobre a resposta anterior, sem dar
                  feedback intermediário, sem dizer "ótimo" ou "interessante".

                COMO ESCOLHER A PRÓXIMA PERGUNTA (MUITO IMPORTANTE):
                - Use TRÊS fontes, nesta ordem de prioridade:
                  1) A RESPOSTA MAIS RECENTE do candidato — é o principal ponto
                     de partida. Aprofunde detalhes técnicos, peça exemplos
                     concretos, desafie suposições, explore trade-offs do que ele
                     acabou de dizer. Encadeie naturalmente: se ele citou Kafka,
                     pergunte sobre Kafka; se mencionou liderança num conflito,
                     aprofunde aquele caso.
                  2) A APRESENTAÇÃO do candidato (1ª resposta dele).
                  3) O CURRÍCULO.
                - Faça follow-ups: 2-3 perguntas encadeadas sobre o mesmo tema
                  antes de pivotar para outra área. Cobertura + variedade, sem
                  pular de tópico sem motivo.
                - NUNCA repita uma pergunta já feita nesta sessão. Confira o
                  histórico antes de perguntar.
                - NUNCA invente fatos que não estão no currículo nem foram ditos
                  pelo candidato.

                ESTILO:
                - Misture comportamentais (STAR) e técnicas. Aumente a profundidade
                  conforme a qualidade das respostas.
                - Português do Brasil, tom profissional, respeitoso e curioso.
                """.formatted(i.getMaxQuestions(), i.getTargetRole(), i.getResumeText());
    }

    private String systemPromptForFeedback(Interview i) {
        return """
                Você é o entrevistador sênior que conduziu esta entrevista para a vaga
                "%s". Agora produza o feedback final do candidato.

                ESCALA DE CLASSIFICAÇÃO (escolha exatamente UMA):
                - "Excelente"   : pronto para a vaga ou nível acima.
                - "Forte"       : atende muito bem, com pequenas lacunas.
                - "Adequado"    : atende, mas tem áreas claras a desenvolver.
                - "Limitado"    : faltam pontos importantes para a vaga.
                - "Insuficiente": muito aquém do esperado para a vaga.

                FORMATO OBRIGATÓRIO — responda SOMENTE com JSON válido,
                sem markdown, sem cercas de código, sem comentários:
                {
                  "classification": "<uma das opções acima>",
                  "summary": "<2 a 4 frases resumindo o desempenho>",
                  "strengths":   ["...", "...", "..."],
                  "gaps":        ["...", "...", "..."],
                  "suggestions": ["...", "...", "..."],
                  "score": <inteiro de 0 a 100>
                }
                """.formatted(i.getTargetRole());
    }

    // ---------------------------------------------------------------- fallbacks
    private String fallbackQuestion(int n) {
        if (n == 1) {
            return "[Modo fallback] "
                    + "Olá! Bem-vindo. Para começarmos, gostaria que você se apresentasse "
                    + "em até 10 minutos: conte sua trajetória, suas principais experiências "
                    + "e o que te trouxe a buscar esta vaga.";
        }
        return "[Modo fallback] Pergunta " + (n - 1)
                + " (até ~4 min de resposta): Conte sobre um projeto desafiador do seu "
                + "currículo, qual foi seu papel, as decisões técnicas que você tomou e o resultado.";
    }

    private InterviewFeedback fallbackFeedback() {
        return new InterviewFeedback(
                "Adequado",
                "Avaliação automática indisponível — conecte sua conta de IA (OAuth) para receber o feedback completo do entrevistador.",
                List.of("Manteve o diálogo até o final"),
                List.of("Profundidade técnica não pôde ser avaliada sem IA"),
                List.of("Conectar conta de IA no menu e refazer a entrevista"),
                60);
    }
}
