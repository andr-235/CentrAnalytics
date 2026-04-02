package com.ca.centranalytics.integration.api.controller;

import com.ca.centranalytics.integration.api.dto.IntegrationSourceResponse;
import com.ca.centranalytics.integration.api.service.IntegrationQueryService;
import com.ca.centranalytics.integration.domain.entity.Platform;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final IntegrationQueryService integrationQueryService;

    @GetMapping
    public List<IntegrationSourceResponse> getIntegrations(@RequestParam(required = false) Platform platform) {
        return integrationQueryService.getIntegrations(platform);
    }
}
