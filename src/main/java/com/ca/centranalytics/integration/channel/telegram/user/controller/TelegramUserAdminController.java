package com.ca.centranalytics.integration.channel.telegram.user.controller;

import com.ca.centranalytics.integration.channel.telegram.user.api.StartTelegramUserSessionRequest;
import com.ca.centranalytics.integration.channel.telegram.user.api.SubmitTelegramAuthCodeRequest;
import com.ca.centranalytics.integration.channel.telegram.user.api.SubmitTelegramPasswordRequest;
import com.ca.centranalytics.integration.channel.telegram.user.api.TelegramUserSessionResponse;
import com.ca.centranalytics.integration.channel.telegram.user.service.TelegramUserAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TelegramUserAdminController {

    private final TelegramUserAuthService telegramUserAuthService;

    @PostMapping("/api/admin/integrations/telegram-user/start")
    public TelegramUserSessionResponse start(@Valid @RequestBody StartTelegramUserSessionRequest request) {
        return telegramUserAuthService.start(request);
    }

    @PostMapping("/api/admin/integrations/telegram-user/{id}/code")
    public TelegramUserSessionResponse submitCode(
            @PathVariable Long id,
            @Valid @RequestBody SubmitTelegramAuthCodeRequest request
    ) {
        return telegramUserAuthService.submitCode(id, request);
    }

    @PostMapping("/api/admin/integrations/telegram-user/{id}/password")
    public TelegramUserSessionResponse submitPassword(
            @PathVariable Long id,
            @Valid @RequestBody SubmitTelegramPasswordRequest request
    ) {
        return telegramUserAuthService.submitPassword(id, request);
    }

    @GetMapping("/api/admin/integrations/telegram-user/{id}/status")
    public TelegramUserSessionResponse getStatus(@PathVariable Long id) {
        return telegramUserAuthService.getStatus(id);
    }

    @GetMapping("/api/admin/integrations/telegram-user/current")
    public TelegramUserSessionResponse getCurrentSession() {
        return telegramUserAuthService.getCurrentSession();
    }
}
