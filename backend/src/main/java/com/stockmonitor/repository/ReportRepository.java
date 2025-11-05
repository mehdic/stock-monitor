package com.stockmonitor.repository;

import com.stockmonitor.model.Report;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

  List<Report> findByUserIdOrderByGenerationTimestampDesc(UUID userId);

  Optional<Report> findByRunId(UUID runId);

  List<Report> findByUserIdAndReportType(UUID userId, String reportType);
}
