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
        name = "vk_comment_snapshot",
        uniqueConstraints = @UniqueConstraint(name = "uk_vk_comment_snapshot_owner_post_comment", columnNames = {"owner_id", "post_id", "comment_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VkCommentSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private IntegrationSource source;

    @Column(name = "author_vk_user_id")
    private Long authorVkUserId;

    @Column(columnDefinition = "text")
    private String text;

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
