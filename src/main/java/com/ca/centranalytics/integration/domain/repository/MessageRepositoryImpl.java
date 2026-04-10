package com.ca.centranalytics.integration.domain.repository;

import com.ca.centranalytics.integration.domain.entity.ExternalUser;
import com.ca.centranalytics.integration.domain.entity.Message;
import com.ca.centranalytics.integration.domain.entity.Platform;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class MessageRepositoryImpl implements MessageRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Message> findMessages(
            Platform platform,
            Long conversationId,
            Long authorId,
            Instant from,
            Instant to,
            String normalizedSearch,
            int offset,
            int limit
    ) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Message> criteriaQuery = criteriaBuilder.createQuery(Message.class);
        Root<Message> message = criteriaQuery.from(Message.class);

        message.fetch("conversation", JoinType.INNER);
        message.fetch("author", JoinType.LEFT);

        Join<Object, Object> conversation = message.join("conversation", JoinType.INNER);
        Join<Message, ExternalUser> author = message.join("author", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();

        if (platform != null) {
            predicates.add(criteriaBuilder.equal(message.get("platform"), platform));
        }

        if (conversationId != null) {
            predicates.add(criteriaBuilder.equal(conversation.get("id"), conversationId));
        }

        if (authorId != null) {
            predicates.add(criteriaBuilder.equal(author.get("id"), authorId));
        }

        if (from != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(message.get("sentAt"), from));
        }

        if (to != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(message.get("sentAt"), to));
        }

        if (normalizedSearch != null && !normalizedSearch.isBlank()) {
            String pattern = "%" + normalizedSearch + "%";
            Predicate textMatch = criteriaBuilder.like(
                    criteriaBuilder.lower(criteriaBuilder.coalesce(message.get("text"), "")),
                    pattern
            );
            Predicate normalizedTextMatch = criteriaBuilder.like(
                    criteriaBuilder.lower(criteriaBuilder.coalesce(message.get("normalizedText"), "")),
                    pattern
            );
            predicates.add(criteriaBuilder.or(textMatch, normalizedTextMatch));
        }

        criteriaQuery.select(message)
                .distinct(true)
                .where(predicates.toArray(Predicate[]::new))
                .orderBy(
                        criteriaBuilder.desc(message.get("sentAt")),
                        criteriaBuilder.desc(message.get("id"))
                );

        TypedQuery<Message> query = entityManager.createQuery(criteriaQuery);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }
}
