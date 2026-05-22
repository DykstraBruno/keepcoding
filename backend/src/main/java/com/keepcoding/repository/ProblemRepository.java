package com.keepcoding.repository;

import com.keepcoding.domain.Problem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
}
