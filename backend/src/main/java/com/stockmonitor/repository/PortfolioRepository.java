package com.stockmonitor.repository;

import com.stockmonitor.model.Portfolio;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {

  Optional<Portfolio> findByUserId(UUID userId);
}
