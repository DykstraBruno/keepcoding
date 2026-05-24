package com.keepcoding.service;

import com.keepcoding.domain.Interview;
import com.keepcoding.domain.InterviewMessage;
import com.keepcoding.domain.enums.MessageRole;
import com.keepcoding.dto.InterviewFeedback;
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
 * <p>O system prompt é construído dinamicamente a cada chamada (não usa
 * {@code defaultSystem}) porque inclui o currículo do candidato.</p>
 *
 * <p>Sem {@code OPENAI_API_KEY} a chamada lança e cai em fallback —
 * mantém o fluxo end-to-end funcional em desenvolvimento.</p>
 */
@Slf4j
@Service
public class InterviewerAiService {

    private final ChatClient chatClient;

    public InterviewerAiService(ChatClient.Builder builder) {
        // Sem defaultSystem: cada call monta seu próprio system prompt.
        this.chatClient = builder.build();
    }

    /** Produz a próxima pergunta do entrevistador, considerando todo o histórico. */
    public String nextQuestion(Interview interview, List<InterviewMessage> history) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPromptForInterview(interview)));
        for (InterviewMessage m : history) {
            messages.add(m.getRole() == MessageRole.INTERVIEWER
                    ? new AssistantMessage(m.getContent())
                    : new UserMessage(m.getContent()));
        }
        if (history.isEmpty()) {
            // Bootstrap: mensagem do usuário para o modelo abrir a entrevista.
            messages.add(new UserMessage(
                    "[INICIAR ENTREVISTA] Faça sua primeira pergunta, personalizada ao currículo."));
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
    public InterviewFeedback produceFeedback(Interview interview, List<InterviewMessage> history) {
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
                Você é um entrevistador profissional sênior do KeepCoding.

                Vaga-alvo: %s

                Currículo do candidato:
                ---
                %s
                ---

                REGRAS DA ENTREVISTA:
                - Cada resposta sua deve ser SOMENTE a próxima pergunta — sem preâmbulo,
                  sem comentário, sem dar feedback intermediário.
                - Faça UMA pergunta por turno. Clara, objetiva, focada em UM tópico.
                - Personalize: cite tecnologias, projetos ou empresas do currículo quando
                  fizer sentido. NUNCA invente fatos que não estão no currículo.
                - Misture perguntas comportamentais (STAR) e técnicas. Aumente a
                  profundidade conforme a qualidade das respostas do candidato.
                - Não dê dicas, respostas ou avaliação durante a entrevista.
                - Português do Brasil, tom profissional, respeitoso e curioso.
                - A entrevista terá aproximadamente %d perguntas no total — equilibre o
                  conjunto para cobrir áreas distintas.
                """.formatted(i.getTargetRole(), i.getResumeText(), i.getMaxQuestions());
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
        return "[Modo fallback — OPENAI_API_KEY não configurada] Pergunta " + n
                + ": Conte sobre um projeto desafiador do seu currículo, qual foi seu"
                + " papel, as decisões técnicas que você tomou e o resultado.";
    }

    private InterviewFeedback fallbackFeedback() {
        return new InterviewFeedback(
                "Adequado",
                "Avaliação automática indisponível — configure OPENAI_API_KEY para receber o feedback completo do entrevistador.",
                List.of("Manteve o diálogo até o final"),
                List.of("Profundidade técnica não pôde ser avaliada sem IA"),
                List.of("Configurar a chave do provedor de IA e refazer a entrevista"),
                60);
    }
}
