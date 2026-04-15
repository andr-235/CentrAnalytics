package com.ca.centranalytics.integration.api.controller;

import com.ca.centranalytics.integration.api.dto.OverviewResponse;
import com.ca.centranalytics.integration.api.dto.OverviewSummaryResponse;
import com.ca.centranalytics.integration.api.service.OverviewQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OverviewControllerIntegrationTest {

    @Test
    void getOverview_returnsPlatformSectionsForSelectedWindow() throws Exception {
        OverviewQueryService service = new OverviewQueryService(null, null, null, null) {
            @Override
            public OverviewResponse getOverview(com.ca.centranalytics.integration.api.dto.OverviewWindow window, Instant now) {
                return new OverviewResponse(
                        Instant.parse("2026-04-10T10:15:00Z"),
                        window.apiValue(),
                        new OverviewSummaryResponse(12, 3, 5, 1),
                        List.of()
                );
            }
        };

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new OverviewController(service)).build();

        mockMvc.perform(get("/api/overview").param("window", "24h"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.window").value("24h"))
                .andExpect(jsonPath("$.summary.messageCount").value(12))
                .andExpect(jsonPath("$.platforms.length()").value(0));
    }
}
