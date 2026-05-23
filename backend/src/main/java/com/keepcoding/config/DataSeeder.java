package com.keepcoding.config;

import com.keepcoding.domain.ArchitectureChallenge;
import com.keepcoding.domain.Problem;
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Popula o banco na primeira execução:
 *  - usuário demo;
 *  - catálogo de problemas de código carregado de /seed/problems.csv;
 *  - desafios de arquitetura.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final ArchitectureChallengeRepository architectureChallengeRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String CATALOG_RESOURCE = "/seed/problems.csv";
    private static final int BATCH_SIZE = 200;

    @Override
    public void run(String... args) {
        seedDemoUser();
        seedProblemCatalog();
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

    // ---------------------------------------------------- catálogo de problemas
    /**
     * Lê o arquivo de catálogo (linhas no formato {@code D|Título},
     * onde D é E/M/H) e persiste em lotes para acelerar a carga inicial.
     */
    private void seedProblemCatalog() {
        if (problemRepository.count() > 0) {
            return;
        }
        InputStream in = getClass().getResourceAsStream(CATALOG_RESOURCE);
        if (in == null) {
            log.warn("[Seed] Recurso {} não encontrado; catálogo não foi carregado.",
                    CATALOG_RESOURCE);
            return;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {

            List<Problem> batch = new ArrayList<>(BATCH_SIZE);
            int total = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                int sep = line.indexOf('|');
                if (sep <= 0) {
                    continue;
                }
                Difficulty difficulty = switch (line.substring(0, sep).trim()) {
                    case "E" -> Difficulty.EASY;
                    case "M" -> Difficulty.MEDIUM;
                    case "H" -> Difficulty.HARD;
                    default -> Difficulty.EASY;
                };
                String title = line.substring(sep + 1).trim();
                if (title.isEmpty()) {
                    continue;
                }

                batch.add(Problem.builder()
                        .title(title)
                        .difficulty(difficulty)
                        .description("Desafio no estilo LeetCode: " + title
                                + ".\n\nResolva o problema e submeta sua solução "
                                + "para receber o feedback do DevCoach.")
                        .timeLimit(2000)
                        .memoryLimit(128000)
                        .build());

                if (batch.size() >= BATCH_SIZE) {
                    problemRepository.saveAll(batch);
                    total += batch.size();
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                problemRepository.saveAll(batch);
                total += batch.size();
            }
            log.info("[Seed] Catálogo de problemas carregado: {} itens.", total);
        } catch (Exception e) {
            log.error("[Seed] Falha ao carregar o catálogo de problemas.", e);
        }
    }

    // ----------------------------------------------- desafios de arquitetura
    private void seedArchitectureChallenges() {
        if (architectureChallengeRepository.count() > 0) {
            return;
        }

        architectureChallengeRepository.save(ArchitectureChallenge.builder()
                .title("Blog Pessoal")
                .difficulty(Difficulty.EASY)
                .context("""
                        Você vai publicar um blog pessoal com artigos sobre programação. \
                        O tráfego é baixo (algumas centenas de visitas por dia) e o conteúdo \
                        muda poucas vezes por semana.""")
                .requirements("""
                        - Servir as páginas com baixa latência.
                        - Custo de operação mínimo.
                        - Fácil de publicar novos artigos.""")
                .build());

        architectureChallengeRepository.save(ArchitectureChallenge.builder()
                .title("Encurtador de URLs")
                .difficulty(Difficulty.MEDIUM)
                .context("""
                        Projete um serviço que recebe uma URL longa e devolve uma URL curta. \
                        Ao acessar a URL curta, o usuário é redirecionado para a original. \
                        O serviço terá leituras (redirecionamentos) muito mais frequentes que \
                        escritas (criação de links).""")
                .requirements("""
                        - Suportar milhões de redirecionamentos por dia.
                        - Redirecionamento com latência muito baixa.
                        - Geração de códigos curtos únicos.
                        - Tolerar picos de leitura sem degradar.""")
                .build());

        architectureChallengeRepository.save(ArchitectureChallenge.builder()
                .title("Feed de Rede Social")
                .difficulty(Difficulty.HARD)
                .context("""
                        Projete o feed de uma rede social: cada usuário vê uma linha do tempo \
                        com as publicações de quem segue. A base tem dezenas de milhões de \
                        usuários, alguns com milhões de seguidores.""")
                .requirements("""
                        - Montar o feed com baixa latência na leitura.
                        - Lidar com usuários "celebridade" (fan-out massivo).
                        - Escalar escrita de publicações e leitura de feeds.
                        - Tolerar falhas de componentes sem derrubar o feed.""")
                .build());

        log.info("[Seed] 3 desafios de arquitetura criados.");
    }
}
