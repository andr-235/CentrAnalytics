package com.ca.centranalytics.integration.domain.repository;

import com.ca.centranalytics.integration.domain.entity.IntegrationAccount;
import com.ca.centranalytics.integration.domain.entity.IntegrationSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IntegrationAccountRepository extends JpaRepository<IntegrationAccount, Long> {
    Optional<IntegrationAccount> findBySourceAndExternalAccountId(IntegrationSource source, String externalAccountId);
}
