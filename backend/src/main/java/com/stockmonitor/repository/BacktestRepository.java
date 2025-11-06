package com.stockmonitor.repository;

import com.stockmonitor.model.Backtest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BacktestRepository extends JpaRepository<Backtest, UUID> {

  List<Backtest> findByUserIdOrderByCreatedAtDesc(UUID userId);

  List<Backtest> findByStatus(String status);

  List<Backtest> findByUserIdAndStatus(UUID userId, String status);
}
