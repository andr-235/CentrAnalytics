package com.ca.centranalytics.integration.channel.telegram.user.service;

import com.ca.centranalytics.integration.channel.telegram.user.config.TelegramUserProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramUserSessionBootstrap {

    private final TelegramUserProperties properties;
    private final TelegramUserSessionService sessionService;
    private final TelegramTdLibClientManager tdLibClientManager;

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrap() {
        if (!properties.enabled()) {
            return;
        }

        sessionService.findReadySession().ifPresent(session -> {
            log.info("Resuming Telegram TDLib session {}", session.getId());
            tdLibClientManager.startOrResume(session);
        });
    }
}
