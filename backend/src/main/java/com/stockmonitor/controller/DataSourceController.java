package com.stockmonitor.controller;

import com.stockmonitor.dto.DataSourceHealthDTO;
import com.stockmonitor.service.DataSourceHealthService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for data source health endpoints (T160).
 *
 * <p>Endpoints: - GET /api/data-sources - Get all data sources with health status - GET
 * /api/data-sources/{id}/health - Get detailed health for specific data source
 */
@RestController
@RequestMapping("/api/data-sources")
@RequiredArgsConstructor
@Slf4j
public class DataSourceController {

  private final DataSourceHealthService dataSourceHealthService;

  /**
   * Get all data sources with health status (FR-037, FR-038).
   *
   * @return List of data sources with health information
   */
  @GetMapping
  @PreAuthorize("hasRole('OWNER') or hasRole('VIEWER')")
  public ResponseEntity<List<DataSourceHealthDTO>> getAllDataSources() {
    log.info("Get all data sources request");
    List<DataSourceHealthDTO> dataSources = dataSourceHealthService.getAllDataSources();
    return ResponseEntity.ok(dataSources);
  }

  /**
   * Get detailed health information for specific data source (FR-037, FR-038).
   *
   * @param id Data source ID (e.g., "market-data", "factor-data")
   * @return Detailed health information
   */
  @GetMapping("/{id}/health")
  @PreAuthorize("hasRole('OWNER') or hasRole('VIEWER')")
  public ResponseEntity<DataSourceHealthDTO> getDataSourceHealth(@PathVariable String id) {
    log.info("Get health for data source: {}", id);
    DataSourceHealthDTO health = dataSourceHealthService.getDataSourceHealth(id);
    return ResponseEntity.ok(health);
  }
}
