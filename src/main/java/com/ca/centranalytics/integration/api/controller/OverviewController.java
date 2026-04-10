package com.ca.centranalytics.integration.api.controller;

import com.ca.centranalytics.integration.api.dto.OverviewResponse;
import com.ca.centranalytics.integration.api.dto.OverviewWindow;
import com.ca.centranalytics.integration.api.service.OverviewQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/overview")
@RequiredArgsConstructor
public class OverviewController {

    private final OverviewQueryService overviewQueryService;

    @GetMapping
    public OverviewResponse getOverview(@RequestParam(defaultValue = "24h") String window) {
        return overviewQueryService.getOverview(OverviewWindow.fromApiValue(window), Instant.now());
    }
}
