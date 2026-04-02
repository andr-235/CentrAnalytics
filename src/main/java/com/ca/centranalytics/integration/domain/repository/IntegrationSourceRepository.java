package com.ca.centranalytics.integration.domain.repository;

import com.ca.centranalytics.integration.domain.entity.IntegrationSource;
import com.ca.centranalytics.integration.domain.entity.Platform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IntegrationSourceRepository extends JpaRepository<IntegrationSource, Long> {
    List<IntegrationSource> findByPlatform(Platform platform);
}
