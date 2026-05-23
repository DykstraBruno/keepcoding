package com.keepcoding.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Popula o banco a cada inicialização (de forma idempotente):
 *  - usuário demo;
 *  - catálogo de problemas de código a partir de /seed/problems.csv;
 *  - desafios de arquitetura a partir de /seed/architecture-challenges.json
 *    (apenas títulos ainda não cadastrados são inseridos).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final ArchitectureChallengeRepository architectureChallengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    private static final String CATALOG_RESOURCE = "/seed/problems.csv";
    private static final String ARCH_CHALLENGES_RESOURCE = "/seed/architecture-challenges.json";
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
     * Lê o arquivo de catálogo (linhas {@code D|Título}, onde D é E/M/H)
     * e persiste em lotes para acelerar a carga inicial. Roda apenas
     * quando a tabela está vazia.
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

    // -------------------------------------------------- desafios de arquitetura
    /**
     * Carrega o catálogo de desafios de arquitetura do JSON. A inserção é
     * idempotente: só insere títulos novos, deixando intactos os já existentes
     * — assim novos desafios podem ser adicionados a cada deploy sem
     * necessidade de resetar o banco.
     */
    private void seedArchitectureChallenges() {
        InputStream in = getClass().getResourceAsStream(ARCH_CHALLENGES_RESOURCE);
        if (in == null) {
            log.warn("[Seed] Recurso {} não encontrado; desafios de arquitetura não foram carregados.",
                    ARCH_CHALLENGES_RESOURCE);
            return;
        }
        try (InputStream input = in) {
            List<ChallengeSeed> seeds = objectMapper.readValue(
                    input, new TypeReference<List<ChallengeSeed>>() {});

            int created = 0;
            int skipped = 0;
            for (ChallengeSeed seed : seeds) {
                if (architectureChallengeRepository.existsByTitle(seed.title())) {
                    skipped++;
                    continue;
                }
                architectureChallengeRepository.save(ArchitectureChallenge.builder()
                        .title(seed.title())
                        .difficulty(Difficulty.valueOf(seed.difficulty()))
                        .context(seed.context())
                        .requirements(seed.requirements())
                        .build());
                created++;
            }
            log.info("[Seed] Desafios de arquitetura: {} criados, {} já existiam.",
                    created, skipped);
        } catch (Exception e) {
            log.error("[Seed] Falha ao carregar os desafios de arquitetura.", e);
        }
    }

    /** Estrutura intermediária para desserializar o JSON dos desafios de arquitetura. */
    private record ChallengeSeed(String title, String difficulty, String context, String requirements) {}
}
