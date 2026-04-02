package com.ca.centranalytics.integration.channel.telegram.user.service;

import com.ca.centranalytics.integration.channel.telegram.user.api.StartTelegramUserSessionRequest;
import com.ca.centranalytics.integration.channel.telegram.user.api.SubmitTelegramAuthCodeRequest;
import com.ca.centranalytics.integration.channel.telegram.user.api.SubmitTelegramPasswordRequest;
import com.ca.centranalytics.integration.channel.telegram.user.api.TelegramUserSessionResponse;
import com.ca.centranalytics.integration.channel.telegram.user.domain.TelegramUserSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TelegramUserAuthService {

    private final TelegramUserSessionService sessionService;
    private final TelegramTdLibClientManager tdLibClientManager;

    @Transactional
    public TelegramUserSessionResponse start(StartTelegramUserSessionRequest request) {
        TelegramUserSession session = sessionService.createSession(request.phoneNumber());
        tdLibClientManager.startOrResume(session);
        return toResponse(sessionService.getSession(session.getId()));
    }

    public TelegramUserSessionResponse submitCode(Long sessionId, SubmitTelegramAuthCodeRequest request) {
        tdLibClientManager.submitCode(sessionId, request.code());
        return toResponse(sessionService.getSession(sessionId));
    }

    public TelegramUserSessionResponse submitPassword(Long sessionId, SubmitTelegramPasswordRequest request) {
        tdLibClientManager.submitPassword(sessionId, request.password());
        return toResponse(sessionService.getSession(sessionId));
    }

    @Transactional(readOnly = true)
    public TelegramUserSessionResponse getStatus(Long sessionId) {
        return toResponse(sessionService.getSession(sessionId));
    }

    @Transactional(readOnly = true)
    public TelegramUserSessionResponse getCurrentSession() {
        return sessionService.findCurrentSession()
                .map(this::toResponse)
                .orElse(null);
    }

    private TelegramUserSessionResponse toResponse(TelegramUserSession session) {
        return new TelegramUserSessionResponse(
                session.getId(),
                session.getPhoneNumber(),
                session.getTelegramUserId(),
                session.getSessionState(),
                session.isAuthorized(),
                session.getErrorMessage(),
                session.getLastSyncAt()
        );
    }
}
