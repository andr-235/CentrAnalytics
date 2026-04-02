package com.ca.centranalytics.integration.channel.telegram.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TelegramUserSessionRepository extends JpaRepository<TelegramUserSession, Long> {
    List<TelegramUserSession> findByAuthorizedTrue();
    Optional<TelegramUserSession> findFirstByAuthorizedTrue();
}
