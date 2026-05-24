package com.keepcoding.repository;

import com.keepcoding.domain.Interview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByUserIdOrderByCreatedAtDesc(Long userId);
}
