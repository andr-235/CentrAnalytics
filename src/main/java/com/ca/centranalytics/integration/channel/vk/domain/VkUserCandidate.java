package com.ca.centranalytics.integration.channel.vk.domain;

import com.ca.centranalytics.integration.domain.entity.IntegrationSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "vk_user_candidate",
        uniqueConstraints = @UniqueConstraint(name = "uk_vk_user_candidate_vk_user_id", columnNames = "vk_user_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VkUserCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vk_user_id", nullable = false)
    private Long vkUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private IntegrationSource source;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "profile_url", length = 1024)
    private String profileUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "region_match_source", nullable = false, length = 32)
    private VkMatchSource regionMatchSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "collection_method", nullable = false, length = 32)
    private VkCollectionMethod collectionMethod;

    @Column(name = "raw_json", nullable = false, columnDefinition = "text")
    private String rawJson;

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
