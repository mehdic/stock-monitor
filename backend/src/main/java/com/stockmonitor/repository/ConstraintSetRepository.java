package com.stockmonitor.repository;

import com.stockmonitor.model.ConstraintSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConstraintSetRepository extends JpaRepository<ConstraintSet, UUID> {

  List<ConstraintSet> findByUserId(UUID userId);

  Optional<ConstraintSet> findByUserIdAndIsActiveTrue(UUID userId);

  List<ConstraintSet> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
