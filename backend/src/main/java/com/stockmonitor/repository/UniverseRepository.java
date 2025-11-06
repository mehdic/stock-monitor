package com.stockmonitor.repository;

import com.stockmonitor.model.Universe;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UniverseRepository extends JpaRepository<Universe, UUID> {

  List<Universe> findByIsActiveTrue();

  Optional<Universe> findByName(String name);
}
