package com.keepcoding.repository;

import com.keepcoding.domain.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByPlayer1IdOrPlayer2IdOrderByStartedAtDesc(Long player1Id, Long player2Id);
}
