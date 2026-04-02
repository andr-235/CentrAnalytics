package com.ca.centranalytics.integration.api.controller;

import com.ca.centranalytics.integration.api.dto.ConversationResponse;
import com.ca.centranalytics.integration.api.service.IntegrationQueryService;
import com.ca.centranalytics.integration.domain.entity.Platform;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final IntegrationQueryService integrationQueryService;

    @GetMapping
    public List<ConversationResponse> getConversations(
            @RequestParam(required = false) Platform platform,
            @RequestParam(required = false) Long sourceId
    ) {
        return integrationQueryService.getConversations(platform, sourceId);
    }
}
