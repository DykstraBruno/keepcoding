package com.keepcoding.repository;

import com.keepcoding.domain.SolvedProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface SolvedProblemRepository extends JpaRepository<SolvedProblem, Long> {

    boolean existsByUserIdAndProblemId(Long userId, Long problemId);

    /** IDs de todos os problemas resolvidos por um usuário. */
    @Query("SELECT s.problem.id FROM SolvedProblem s WHERE s.user.id = :userId")
    Set<Long> findSolvedProblemIdsByUserId(@Param("userId") Long userId);
}
