package com.ca.centranalytics.integration.channel.telegram.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "telegram_user_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelegramUserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, length = 64)
    private String phoneNumber;

    @Column(name = "telegram_user_id")
    private Long telegramUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_state", nullable = false, length = 32)
    private TelegramUserSessionState sessionState;

    @Column(name = "tdlib_database_path", nullable = false, length = 1024)
    private String tdlibDatabasePath;

    @Column(name = "tdlib_files_path", nullable = false, length = 1024)
    private String tdlibFilesPath;

    @Column(name = "is_authorized", nullable = false)
    private boolean authorized;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
