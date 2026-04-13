package com.ca.centranalytics.integration.domain.repository;

import com.ca.centranalytics.integration.api.dto.OverviewWindow;
import com.ca.centranalytics.integration.domain.entity.Platform;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class OverviewMetricsRepositoryImpl implements OverviewMetricsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public OverviewSummaryMetrics fetchSummary(Instant from, Instant to) {
        Object[] row = (Object[]) entityManager.createQuery("""
                select count(m.id),
                       count(distinct m.conversation.id),
                       count(distinct m.author.id)
                from Message m
                where m.sentAt between :from and :to
                """)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();

        return new OverviewSummaryMetrics(
                numberValue(row[0]),
                numberValue(row[1]),
                numberValue(row[2])
        );
    }

    @Override
    public List<OverviewPlatformMetrics> fetchPlatformMetrics(Instant from, Instant to) {
        return entityManager.createQuery("""
                select new com.ca.centranalytics.integration.domain.repository.OverviewPlatformMetrics(
                    m.platform,
                    count(m.id),
                    count(distinct m.conversation.id),
                    count(distinct m.author.id),
                    max(m.sentAt)
                )
                from Message m
                where m.sentAt between :from and :to
                group by m.platform
                """, OverviewPlatformMetrics.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    @Override
    public List<OverviewTrendBucket> fetchTrend(Platform platform, Instant from, Instant to, OverviewWindow window) {
        String bucket = window == OverviewWindow.HOURS_24 ? "hour" : "day";

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                select date_trunc(:bucket, m.sent_at) as bucket_start,
                       count(*) as message_count
                from message m
                where m.platform = :platform
                  and m.sent_at between :from and :to
                group by bucket_start
                order by bucket_start
                """)
                .setParameter("bucket", bucket)
                .setParameter("platform", platform.name())
                .setParameter("from", Timestamp.from(from))
                .setParameter("to", Timestamp.from(to))
                .getResultList();

        return rows.stream()
                .map(row -> new OverviewTrendBucket(
                        instantValue(row[0]),
                        numberValue(row[1])
                ))
                .toList();
    }

    private Instant instantValue(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        throw new IllegalArgumentException("Unsupported overview bucket value: " + value);
    }

    private long numberValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
