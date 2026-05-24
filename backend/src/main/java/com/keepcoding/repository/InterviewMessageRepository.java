package com.keepcoding.repository;

import com.keepcoding.domain.InterviewMessage;
import com.keepcoding.domain.enums.MessageRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewMessageRepository extends JpaRepository<InterviewMessage, Long> {

    List<InterviewMessage> findByInterviewIdOrderByTurnIndexAsc(Long interviewId);

    long countByInterviewIdAndRole(Long interviewId, MessageRole role);
}
