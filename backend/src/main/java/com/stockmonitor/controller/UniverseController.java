package com.stockmonitor.controller;

import com.stockmonitor.dto.UniverseDTO;
import com.stockmonitor.dto.UniverseSelectionResponse;
import com.stockmonitor.service.UniverseService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Universe controller handling universe selection and retrieval.
 *
 * <p>Endpoints: - GET /api/universes - Get all active universes - GET /api/universes/{id} - Get
 * universe by ID - PUT /api/portfolios/{portfolioId}/universe - Select universe for portfolio
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class UniverseController {

  private final UniverseService universeService;

  @GetMapping("/api/universes")
  public ResponseEntity<List<UniverseDTO>> getAllUniverses() {
    log.info("Get all universes request");
    List<UniverseDTO> universes = universeService.getAllUniverses();
    return ResponseEntity.ok(universes);
  }

  @GetMapping("/api/universes/{id}")
  public ResponseEntity<UniverseDTO> getUniverse(@PathVariable UUID id) {
    log.info("Get universe request for ID: {}", id);
    UniverseDTO universe = universeService.getUniverse(id);
    return ResponseEntity.ok(universe);
  }

  @PutMapping("/api/portfolios/{portfolioId}/universe")
  public ResponseEntity<UniverseSelectionResponse> selectUniverse(
      @PathVariable UUID portfolioId, @RequestParam UUID universeId) {
    log.info("Select universe {} for portfolio {}", universeId, portfolioId);
    UniverseSelectionResponse response =
        universeService.selectUniverseForPortfolio(portfolioId, universeId);
    return ResponseEntity.ok(response);
  }
}
