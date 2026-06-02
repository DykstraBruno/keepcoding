package com.keepcoding.repository;

import com.keepcoding.domain.OAuthToken;
import com.keepcoding.domain.enums.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OAuthTokenRepository extends JpaRepository<OAuthToken, Long> {

    Optional<OAuthToken> findByUserIdAndProvider(Long userId, OAuthProvider provider);

    List<OAuthToken> findByUserId(Long userId);

    void deleteByUserIdAndProvider(Long userId, OAuthProvider provider);
}
