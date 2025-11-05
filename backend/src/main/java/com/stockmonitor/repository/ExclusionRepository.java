package com.stockmonitor.repository;

import com.stockmonitor.model.Exclusion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExclusionRepository extends JpaRepository<Exclusion, UUID> {

  List<Exclusion> findByRunId(UUID runId);

  List<Exclusion> findByRunIdAndExclusionReasonCode(UUID runId, String exclusionReasonCode);

  void deleteByRunId(UUID runId);
}
