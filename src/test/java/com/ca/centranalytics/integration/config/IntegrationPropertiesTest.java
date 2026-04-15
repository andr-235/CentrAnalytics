package com.ca.centranalytics.integration.config;

import com.ca.centranalytics.integration.channel.vk.config.VkAutoCollectionProperties;
import com.ca.centranalytics.integration.channel.vk.config.VkProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = IntegrationPropertiesTest.TestConfig.class)
@TestPropertySource(properties = {
        "integration.vk.group-id=42",
        "integration.vk.access-token=vk-token",
        "integration.vk.api-version=5.199",
        "integration.vk.api-base-url=https://api.vk.com/method",
        "integration.vk.request-timeout=5s",
        "integration.vk.minimum-request-interval=1s",
        "integration.vk.rate-limit-retry-attempts=5",
        "integration.vk.rate-limit-retry-base-delay=2s",
        "integration.vk.rate-limit-retry-max-delay=30s",
        "integration.vk.auto-collection.enabled=true",
        "integration.vk.auto-collection.region=Primorsky Krai",
        "integration.vk.auto-collection.group-search-limit=25",
        "integration.vk.auto-collection.post-limit=10",
        "integration.vk.auto-collection.comment-post-limit=5",
        "integration.vk.auto-collection.comment-limit=20",
        "integration.vk.auto-collection.fixed-delay-ms=900000",
        "integration.max.webhook-path=/api/integrations/webhooks/wappi/max"
})
class IntegrationPropertiesTest {

    @Autowired
    private VkProperties vkProperties;

    @Autowired
    private VkAutoCollectionProperties vkAutoCollectionProperties;

    @Autowired
    private Environment environment;

    @Test
    void bindsVkDiscoveryProperties() {
        assertThat(vkProperties.groupId()).isEqualTo(42L);
        assertThat(vkProperties.accessToken()).isEqualTo("vk-token");
        assertThat(vkProperties.apiVersion()).isEqualTo("5.199");
        assertThat(vkProperties.apiBaseUrl()).isEqualTo("https://api.vk.com/method");
        assertThat(vkProperties.requestTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(vkProperties.minimumRequestInterval()).isEqualTo(Duration.ofSeconds(1));
        assertThat(vkProperties.rateLimitRetryAttempts()).isEqualTo(5);
        assertThat(vkProperties.rateLimitRetryBaseDelay()).isEqualTo(Duration.ofSeconds(2));
        assertThat(vkProperties.rateLimitRetryMaxDelay()).isEqualTo(Duration.ofSeconds(30));
        assertThat(vkAutoCollectionProperties.enabled()).isTrue();
        assertThat(vkAutoCollectionProperties.region()).isEqualTo("Primorsky Krai");
        assertThat(vkAutoCollectionProperties.groupSearchLimit()).isEqualTo(25);
        assertThat(vkAutoCollectionProperties.postLimit()).isEqualTo(10);
        assertThat(vkAutoCollectionProperties.commentPostLimit()).isEqualTo(5);
        assertThat(vkAutoCollectionProperties.commentLimit()).isEqualTo(20);
        assertThat(vkAutoCollectionProperties.fixedDelayMs()).isEqualTo(900000L);
    }

    @Test
    void exposesMaxWebhookPathProperty() {
        assertThat(environment.getProperty("integration.max.webhook-path"))
                .isEqualTo("/api/integrations/webhooks/wappi/max");
    }

    @Configuration
    @EnableConfigurationProperties({VkProperties.class, VkAutoCollectionProperties.class})
    static class TestConfig {
    }
}
