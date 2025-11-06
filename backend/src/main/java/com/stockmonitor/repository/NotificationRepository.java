package com.stockmonitor.repository;

import com.stockmonitor.model.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  List<Notification> findByUserIdOrderBySentAtDesc(UUID userId);

  List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

  List<Notification> findByUserIdAndIsReadFalseOrderBySentAtDesc(UUID userId);

  List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);

  List<Notification> findByUserIdAndIsReadFalse(UUID userId);

  long countByUserIdAndIsReadFalse(UUID userId);

  List<Notification> findByRunId(UUID runId);
}
