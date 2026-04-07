package com.ca.centranalytics.integration.config;

import com.ca.centranalytics.integration.channel.telegram.user.config.TelegramUserProperties;
import com.ca.centranalytics.integration.channel.vk.config.VkProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = IntegrationPropertiesTest.TestConfig.class)
@TestPropertySource(properties = {
        "integration.vk.group-id=42",
        "integration.vk.access-token=vk-token",
        "integration.vk.user-access-token=vk-user-token",
        "integration.vk.api-version=5.199",
        "integration.vk.api-base-url=https://api.vk.com/method",
        "integration.vk.fallback-base-url=https://vk.com",
        "integration.vk.request-timeout=5s",
        "integration.telegram-user.enabled=true",
        "integration.telegram-user.api-id=12345",
        "integration.telegram-user.api-hash=hash-value",
        "integration.telegram-user.database-dir=.tdlib/db",
        "integration.telegram-user.files-dir=.tdlib/files",
        "integration.telegram-user.system-language-code=ru",
        "integration.telegram-user.device-model=CentrAnalytics",
        "integration.telegram-user.system-version=server",
        "integration.telegram-user.application-version=1.0.0"
})
class IntegrationPropertiesTest {

    @Autowired
    private VkProperties vkProperties;

    @Autowired
    private TelegramUserProperties telegramUserProperties;

    @Test
    void bindsVkDiscoveryAndTelegramUserProperties() {
        assertThat(vkProperties.groupId()).isEqualTo(42L);
        assertThat(vkProperties.accessToken()).isEqualTo("vk-token");
        assertThat(vkProperties.userAccessToken()).isEqualTo("vk-user-token");
        assertThat(vkProperties.apiVersion()).isEqualTo("5.199");
        assertThat(vkProperties.apiBaseUrl()).isEqualTo("https://api.vk.com/method");
        assertThat(vkProperties.fallbackBaseUrl()).isEqualTo("https://vk.com");
        assertThat(vkProperties.requestTimeout()).isEqualTo(Duration.ofSeconds(5));

        assertThat(telegramUserProperties.enabled()).isTrue();
        assertThat(telegramUserProperties.apiId()).isEqualTo(12345);
        assertThat(telegramUserProperties.apiHash()).isEqualTo("hash-value");
        assertThat(telegramUserProperties.databaseDir()).isEqualTo(".tdlib/db");
        assertThat(telegramUserProperties.filesDir()).isEqualTo(".tdlib/files");
        assertThat(telegramUserProperties.systemLanguageCode()).isEqualTo("ru");
        assertThat(telegramUserProperties.deviceModel()).isEqualTo("CentrAnalytics");
        assertThat(telegramUserProperties.systemVersion()).isEqualTo("server");
        assertThat(telegramUserProperties.applicationVersion()).isEqualTo("1.0.0");
    }

    @Configuration
    @EnableConfigurationProperties({VkProperties.class, TelegramUserProperties.class})
    static class TestConfig {
    }
}
