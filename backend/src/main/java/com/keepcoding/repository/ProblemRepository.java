package com.keepcoding.repository;

import com.keepcoding.domain.Problem;
import com.keepcoding.domain.enums.Difficulty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

    List<Problem> findByDifficulty(Difficulty difficulty);
}
