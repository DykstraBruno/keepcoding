package com.keepcoding.repository;

import com.keepcoding.domain.ArchitectureChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArchitectureChallengeRepository extends JpaRepository<ArchitectureChallenge, Long> {

    boolean existsByTitle(String title);
}
