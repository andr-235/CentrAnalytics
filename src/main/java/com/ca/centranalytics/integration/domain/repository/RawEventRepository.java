package com.ca.centranalytics.integration.domain.repository;

import com.ca.centranalytics.integration.domain.entity.Platform;
import com.ca.centranalytics.integration.domain.entity.RawEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RawEventRepository extends JpaRepository<RawEvent, Long> {
    Optional<RawEvent> findByPlatformAndEventId(Platform platform, String eventId);
}
