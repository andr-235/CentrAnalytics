package com.ca.centranalytics.integration.domain.repository;

import com.ca.centranalytics.integration.domain.entity.IngestionCheckpoint;
import com.ca.centranalytics.integration.domain.entity.IntegrationSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IngestionCheckpointRepository extends JpaRepository<IngestionCheckpoint, Long> {
    Optional<IngestionCheckpoint> findBySourceAndCheckpointType(IntegrationSource source, String checkpointType);
}
