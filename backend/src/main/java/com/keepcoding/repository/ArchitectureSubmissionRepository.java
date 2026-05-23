package com.keepcoding.repository;

import com.keepcoding.domain.ArchitectureSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArchitectureSubmissionRepository extends JpaRepository<ArchitectureSubmission, Long> {

    List<ArchitectureSubmission> findByUserIdOrderByCreatedAtDesc(Long userId);
}
