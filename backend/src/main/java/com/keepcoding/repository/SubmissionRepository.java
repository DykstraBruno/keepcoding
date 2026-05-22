package com.keepcoding.repository;

import com.keepcoding.domain.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Submission> findByProblemId(Long problemId);
}
