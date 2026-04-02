package com.ca.centranalytics.integration.api.controller;

import com.ca.centranalytics.integration.api.dto.ExternalUserResponse;
import com.ca.centranalytics.integration.api.service.IntegrationQueryService;
import com.ca.centranalytics.integration.domain.entity.Platform;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ExternalUserController {

    private final IntegrationQueryService integrationQueryService;

    @GetMapping
    public List<ExternalUserResponse> getUsers(
            @RequestParam(required = false) Platform platform,
            @RequestParam(required = false) String search
    ) {
        return integrationQueryService.getUsers(platform, search);
    }
}
