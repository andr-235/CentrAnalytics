package com.ca.centranalytics.integration.channel.telegram.user.service;

import com.ca.centranalytics.integration.api.exception.IntegrationNotFoundException;
import com.ca.centranalytics.integration.channel.telegram.user.config.TelegramUserProperties;
import com.ca.centranalytics.integration.channel.telegram.user.domain.TelegramUserSession;
import com.ca.centranalytics.integration.channel.telegram.user.domain.TelegramUserSessionRepository;
import com.ca.centranalytics.integration.channel.telegram.user.domain.TelegramUserSessionState;
import com.ca.centranalytics.integration.channel.telegram.user.exception.TelegramUserSessionConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TelegramUserSessionService {

    private final TelegramUserSessionRepository repository;
    private final TelegramUserProperties properties;

    @Transactional
    public TelegramUserSession createSession(String phoneNumber) {
        String normalizedPhone = normalizePhoneNumber(phoneNumber);
        ensureNoActiveSessions();

        TelegramUserSession session = TelegramUserSession.builder()
                .phoneNumber(normalizedPhone)
                .sessionState(TelegramUserSessionState.WAIT_PHONE)
                .tdlibDatabasePath(resolvePath(properties.databaseDir(), normalizedPhone).toString())
                .tdlibFilesPath(resolvePath(properties.filesDir(), normalizedPhone).toString())
                .authorized(false)
                .build();

        return repository.save(session);
    }

    @Transactional(readOnly = true)
    public TelegramUserSession getSession(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IntegrationNotFoundException("Telegram user session " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public Optional<TelegramUserSession> findReadySession() {
        return repository.findFirstByAuthorizedTrue();
    }

    @Transactional(readOnly = true)
    public Optional<TelegramUserSession> findCurrentSession() {
        return repository.findAll().stream()
                .filter(this::isActive)
                .sorted(Comparator.comparing(TelegramUserSession::getUpdatedAt).reversed())
                .findFirst();
    }

    @Transactional
    public TelegramUserSession updateState(Long id, TelegramUserSessionState state, String errorMessage) {
        TelegramUserSession session = getSession(id);
        session.setSessionState(state);
        session.setErrorMessage(errorMessage);
        if (state != TelegramUserSessionState.READY) {
            session.setAuthorized(false);
            if (state == TelegramUserSessionState.FAILED) {
                session.setTelegramUserId(null);
            }
        }
        return repository.save(session);
    }

    @Transactional
    public TelegramUserSession markReady(Long id, Long telegramUserId) {
        TelegramUserSession session = getSession(id);
        session.setTelegramUserId(telegramUserId);
        session.setSessionState(TelegramUserSessionState.READY);
        session.setAuthorized(true);
        session.setErrorMessage(null);
        session.setLastSyncAt(Instant.now());
        return repository.save(session);
    }

    @Transactional
    public void markFailed(Long id, String errorMessage) {
        TelegramUserSession session = getSession(id);
        session.setSessionState(TelegramUserSessionState.FAILED);
        session.setAuthorized(false);
        session.setErrorMessage(errorMessage);
        repository.save(session);
    }

    @Transactional
    public void touchSync(Long id) {
        TelegramUserSession session = getSession(id);
        session.setLastSyncAt(Instant.now());
        repository.save(session);
    }

    @Transactional(readOnly = true)
    public List<TelegramUserSession> findAll() {
        return repository.findAll();
    }

    private void ensureNoActiveSessions() {
        boolean hasActiveSession = repository.findAll().stream().anyMatch(this::isActive);
        if (hasActiveSession) {
            throw new TelegramUserSessionConflictException("Only one Telegram user session is supported in this MVP");
        }
    }

    private boolean isActive(TelegramUserSession session) {
        return session.isAuthorized() || session.getSessionState() != TelegramUserSessionState.FAILED;
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) {
            throw new IllegalArgumentException("phoneNumber is required");
        }
        String normalized = phoneNumber.replace(" ", "").replace("-", "").replace("(", "").replace(")", "");
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("phoneNumber is required");
        }
        return normalized;
    }

    private Path resolvePath(String baseDir, String phoneNumber) {
        String phoneSegment = phoneNumber.replaceAll("[^0-9A-Za-z]+", "_");
        return Path.of(baseDir).resolve(phoneSegment);
    }
}
