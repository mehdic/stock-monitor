package com.stockmonitor.repository;

import com.stockmonitor.model.RecommendationRun;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendationRunRepository extends JpaRepository<RecommendationRun, UUID> {

  List<RecommendationRun> findByUserIdOrderByCreatedAtDesc(UUID userId);

  Optional<RecommendationRun> findFirstByUserIdAndStatusOrderByCompletedAtDesc(
      UUID userId, String status);

  List<RecommendationRun> findByUserIdAndStatusOrderByCompletedAtDesc(UUID userId, String status);

  List<RecommendationRun> findByScheduledDate(LocalDate scheduledDate);

  List<RecommendationRun> findByScheduledDateAndRunType(LocalDate scheduledDate, String runType);

  List<RecommendationRun> findByScheduledDateAndStatus(LocalDate scheduledDate, String status);

  List<RecommendationRun> findByStatus(String status);

  @Query(
      "SELECT r FROM RecommendationRun r WHERE r.userId = :userId AND r.status = 'FINALIZED' ORDER BY r.completedAt DESC")
  List<RecommendationRun> findCompletedRunsByUser(UUID userId);

  List<RecommendationRun> findByUserIdAndRunTypeOrderByCreatedAtDesc(UUID userId, String runType);

  List<RecommendationRun> findByRunTypeOrderByCreatedAtDesc(String runType);

  @Query(
      "SELECT r FROM RecommendationRun r JOIN Portfolio p ON r.userId = p.userId WHERE p.id = :portfolioId AND r.runType = 'SCHEDULED' AND r.status = 'COMPLETED' ORDER BY r.completedAt DESC")
  List<RecommendationRun> findLatestScheduledRunForPortfolio(UUID portfolioId);
}
