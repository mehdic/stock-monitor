package com.stockmonitor.repository;

import com.stockmonitor.model.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

  List<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId);

  List<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId);

  @Query(
      "SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.createdAt >= :startDate ORDER BY a.createdAt DESC")
  List<AuditLog> findUserActivitySince(UUID userId, LocalDateTime startDate);

  List<AuditLog> findBySessionId(String sessionId);
}
