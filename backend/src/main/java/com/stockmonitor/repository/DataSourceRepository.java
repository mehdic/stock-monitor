package com.stockmonitor.repository;

import com.stockmonitor.model.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, UUID> {

  List<DataSource> findByIsActiveTrue();

  List<DataSource> findByIsCriticalTrue();

  Optional<DataSource> findByName(String name);

  List<DataSource> findByHealthStatus(String healthStatus);
}
