package com.ca.centranalytics.integration.domain.repository;

import com.ca.centranalytics.integration.domain.entity.ExternalUser;
import com.ca.centranalytics.integration.domain.entity.Platform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExternalUserRepository extends JpaRepository<ExternalUser, Long> {
    Optional<ExternalUser> findByPlatformAndExternalUserId(Platform platform, String externalUserId);
    List<ExternalUser> findByPlatform(Platform platform);
}
