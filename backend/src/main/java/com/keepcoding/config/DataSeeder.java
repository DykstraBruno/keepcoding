package com.keepcoding.config;

import com.keepcoding.domain.ArchitectureChallenge;
import com.keepcoding.domain.Problem;
import com.keepcoding.domain.TestCase;
import com.keepcoding.domain.User;
import com.keepcoding.domain.enums.Difficulty;
import com.keepcoding.domain.enums.TierPlan;
import com.keepcoding.repository.ArchitectureChallengeRepository;
import com.keepcoding.repository.ProblemRepository;
import com.keepcoding.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Popula o banco na primeira execução: usuário demo, problemas de código
 * e desafios de arquitetura.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final ArchitectureChallengeRepository architectureChallengeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedDemoUser();
        seedProblems();
        seedArchitectureChallenges();
    }

    // ------------------------------------------------------------------ usuário
    private void seedDemoUser() {
        if (userRepository.count() > 0) {
            return;
        }
        userRepository.save(User.builder()
                .username("demo")
                .email("demo@keepcoding.dev")
                .password(passwordEncoder.encode("demo"))
                .xp(0)
                .tierPlan(TierPlan.FREE)
                .build());
        log.info("[Seed] Usuário 'demo' criado.");
    }

    // ----------------------------------------------------------------- problemas
    private void seedProblems() {
        if (problemRepository.count() > 0) {
            return;
        }

        problemRepository.save(problem("Two Sum", Difficulty.EASY, """
                Dado um array de inteiros `nums` e um inteiro `target`, retorne os índices \
                dos dois números cuja soma é igual a `target`. Cada entrada tem exatamente \
                uma solução e o mesmo elemento não pode ser usado duas vezes.""",
                tc("[2,7,11,15]\n9", "[0,1]", true),
                tc("[3,2,4]\n6", "[1,2]", true),
                tc("[3,3]\n6", "[0,1]", false)));

        problemRepository.save(problem("Valid Parentheses", Difficulty.EASY, """
                Dada uma string contendo apenas os caracteres '()[]{}', determine se \
                ela está balanceada: todo parêntese aberto é fechado pelo mesmo tipo \
                e na ordem correta.""",
                tc("()[]{}", "true", true),
                tc("(]", "false", true),
                tc("([)]", "false", false)));

        problemRepository.save(problem("Longest Substring Without Repeating Characters",
                Difficulty.MEDIUM, """
                Dada uma string `s`, encontre o comprimento da maior substring que não \
                contém caracteres repetidos.""",
                tc("abcabcbb", "3", true),
                tc("bbbbb", "1", true),
                tc("pwwkew", "3", false)));

        problemRepository.save(problem("LRU Cache", Difficulty.MEDIUM, """
                Implemente uma estrutura de cache com política LRU (Least Recently Used) \
                e capacidade fixa, suportando `get` e `put` em tempo médio O(1).""",
                tc("put(1,1) put(2,2) get(1)", "1", true),
                tc("capacity=2 put(1,1) put(2,2) put(3,3) get(2)", "-1", false)));

        problemRepository.save(problem("Merge k Sorted Lists", Difficulty.HARD, """
                Dadas `k` listas encadeadas já ordenadas, faça o merge de todas em uma \
                única lista ordenada e retorne-a.""",
                tc("[[1,4,5],[1,3,4],[2,6]]", "[1,1,2,3,4,4,5,6]", true),
                tc("[]", "[]", true),
                tc("[[]]", "[]", false)));

        log.info("[Seed] 5 problemas de código criados.");
    }

    // ------------------------------------------------------ desafios de arquitetura
    private void seedArchitectureChallenges() {
        if (architectureChallengeRepository.count() > 0) {
            return;
        }

        architectureChallengeRepository.save(archChallenge("Blog Pessoal", Difficulty.EASY,
                """
                Você vai publicar um blog pessoal com artigos sobre programação. \
                O tráfego é baixo (algumas centenas de visitas por dia) e o conteúdo \
                muda poucas vezes por semana.""",
                """
                - Servir as páginas com baixa latência.
                - Custo de operação mínimo.
                - Fácil de publicar novos artigos."""));

        architectureChallengeRepository.save(archChallenge("Encurtador de URLs", Difficulty.MEDIUM,
                """
                Projete um serviço que recebe uma URL longa e devolve uma URL curta. \
                Ao acessar a URL curta, o usuário é redirecionado para a original. \
                O serviço terá leituras (redirecionamentos) muito mais frequentes que \
                escritas (criação de links).""",
                """
                - Suportar milhões de redirecionamentos por dia.
                - Redirecionamento com latência muito baixa.
                - Geração de códigos curtos únicos.
                - Tolerar picos de leitura sem degradar."""));

        architectureChallengeRepository.save(archChallenge("Feed de Rede Social", Difficulty.HARD,
                """
                Projete o feed de uma rede social: cada usuário vê uma linha do tempo \
                com as publicações de quem segue. A base tem dezenas de milhões de \
                usuários, alguns com milhões de seguidores.""",
                """
                - Montar o feed com baixa latência na leitura.
                - Lidar com usuários "celebridade" (fan-out massivo).
                - Escalar escrita de publicações e leitura de feeds.
                - Tolerar falhas de componentes sem derrubar o feed."""));

        log.info("[Seed] 3 desafios de arquitetura criados.");
    }

    // --------------------------------------------------------------------- helpers
    private static Problem problem(String title, Difficulty difficulty, String description, Tc... cases) {
        Problem problem = Problem.builder()
                .title(title)
                .difficulty(difficulty)
                .description(description)
                .timeLimit(2000)
                .memoryLimit(128000)
                .build();
        for (Tc c : cases) {
            problem.getTestCases().add(TestCase.builder()
                    .problem(problem)
                    .input(c.input())
                    .expectedOutput(c.output())
                    .isSample(c.sample())
                    .build());
        }
        return problem;
    }

    private static Tc tc(String input, String output, boolean sample) {
        return new Tc(input, output, sample);
    }

    /** Semente de caso de teste. */
    private record Tc(String input, String output, boolean sample) {}

    private static ArchitectureChallenge archChallenge(String title, Difficulty difficulty,
                                                       String context, String requirements) {
        return ArchitectureChallenge.builder()
                .title(title)
                .difficulty(difficulty)
                .context(context)
                .requirements(requirements)
                .build();
    }
}
