package com.stockmonitor.repository;

import com.stockmonitor.model.ServiceApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceApiKeyRepository extends JpaRepository<ServiceApiKey, UUID> {

    Optional<ServiceApiKey> findByKeyHash(String keyHash);

    List<ServiceApiKey> findByIsActiveTrue();
}
