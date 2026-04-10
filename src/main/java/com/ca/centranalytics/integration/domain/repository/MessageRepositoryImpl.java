package com.ca.centranalytics.integration.domain.repository;

import com.ca.centranalytics.integration.api.dto.MessageResponse;
import com.ca.centranalytics.integration.domain.entity.Platform;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class MessageRepositoryImpl implements MessageRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<MessageResponse> findMessageResponses(
            Platform platform,
            Long conversationId,
            Long authorId,
            Instant from,
            Instant to,
            String normalizedSearch,
            int offset,
            int limit
    ) {
        TypedQuery<MessageResponse> query = entityManager.createQuery("""
                select new com.ca.centranalytics.integration.api.dto.MessageResponse(
                    m.id,
                    m.platform,
                    m.externalMessageId,
                    c.id,
                    c.title,
                    c.externalConversationId,
                    c.type,
                    a.id,
                    a.displayName,
                    a.username,
                    a.externalUserId,
                    a.phone,
                    m.text,
                    m.messageType,
                    m.sentAt
                )
                from Message m
                join m.conversation c
                left join m.author a
                where (:platform is null or m.platform = :platform)
                  and (:conversationId is null or c.id = :conversationId)
                  and (:authorId is null or a.id = :authorId)
                  and (:from is null or m.sentAt >= :from)
                  and (:to is null or m.sentAt <= :to)
                  and (
                    :normalizedSearch is null
                    or lower(coalesce(m.text, '')) like concat('%', :normalizedSearch, '%')
                    or lower(coalesce(m.normalizedText, '')) like concat('%', :normalizedSearch, '%')
                  )
                order by m.sentAt desc, m.id desc
                """, MessageResponse.class);

        query.setParameter("platform", platform);
        query.setParameter("conversationId", conversationId);
        query.setParameter("authorId", authorId);
        query.setParameter("from", from);
        query.setParameter("to", to);
        query.setParameter("normalizedSearch", normalizedSearch);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }
}
