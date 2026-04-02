package com.ca.centranalytics.integration.api;

import com.ca.centranalytics.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "integration.telegram.webhook-secret=tg-secret"
})
class IntegrationErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsStableErrorForInvalidWebhookSecret() throws Exception {
        mockMvc.perform(post("/api/integrations/webhooks/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Telegram-Bot-Api-Secret-Token", "wrong")
                        .content("""
                                {"update_id":1,"message":{"message_id":1,"date":1775088000,"text":"hello","chat":{"id":100,"type":"group","title":"TG"},"from":{"id":55,"is_bot":false,"first_name":"Petr"}}}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Telegram webhook secret is invalid"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void returnsStableErrorForMissingRawEvent() throws Exception {
        mockMvc.perform(get("/api/raw-events/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Raw event not found: 999999"));
    }
}
