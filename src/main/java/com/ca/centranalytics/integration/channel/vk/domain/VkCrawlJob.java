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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "vk_crawl_job")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VkCrawlJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private IntegrationSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 64)
    private VkCrawlJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VkCrawlJobStatus status;

    @Column(name = "request_json", nullable = false, columnDefinition = "text")
    private String requestJson;

    @Column(name = "result_json", columnDefinition = "text")
    private String resultJson;

    @Column(name = "item_count", nullable = false)
    private int itemCount;

    @Column(name = "processed_count", nullable = false)
    private int processedCount;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(name = "warning_count", nullable = false)
    private int warningCount;

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
