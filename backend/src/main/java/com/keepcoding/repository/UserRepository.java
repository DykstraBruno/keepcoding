package com.keepcoding.repository;

import com.keepcoding.domain.User;
import com.keepcoding.dto.RankingRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    /**
     * Ranking de todos os usuários: XP total + quantidade de problemas resolvidos
     * por dificuldade. Ordenado por XP desc; desempate por username asc.
     * Usuários sem nenhuma resolução aparecem com zeros (LEFT JOIN).
     */
    /**
     * Usa joins explícitos LEFT (incluindo para Problem) para preservar
     * usuários sem solved_problems no resultado — path implícito
     * {@code sp.problem.difficulty} dentro de SUM/CASE acabaria gerando
     * INNER JOIN e filtraria esses usuários.
     */
    @Query("""
            SELECT new com.keepcoding.dto.RankingRow(
                u.id, u.username, u.xp,
                SUM(CASE WHEN p.difficulty = com.keepcoding.domain.enums.Difficulty.EASY   THEN 1L ELSE 0L END),
                SUM(CASE WHEN p.difficulty = com.keepcoding.domain.enums.Difficulty.MEDIUM THEN 1L ELSE 0L END),
                SUM(CASE WHEN p.difficulty = com.keepcoding.domain.enums.Difficulty.HARD   THEN 1L ELSE 0L END))
            FROM User u
            LEFT JOIN SolvedProblem sp ON sp.user = u
            LEFT JOIN Problem p ON sp.problem = p
            GROUP BY u.id, u.username, u.xp
            ORDER BY u.xp DESC, u.username ASC
            """)
    List<RankingRow> findRankingByXp();
}
