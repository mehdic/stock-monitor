package com.stockmonitor.service;

import com.stockmonitor.dto.UniverseDTO;
import com.stockmonitor.dto.UniverseSelectionResponse;
import com.stockmonitor.model.Holding;
import com.stockmonitor.model.Portfolio;
import com.stockmonitor.model.Universe;
import com.stockmonitor.model.UniverseConstituent;
import com.stockmonitor.repository.HoldingRepository;
import com.stockmonitor.repository.PortfolioRepository;
import com.stockmonitor.repository.UniverseConstituentRepository;
import com.stockmonitor.repository.UniverseRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UniverseService {

  private final UniverseRepository universeRepository;
  private final UniverseConstituentRepository universeConstituentRepository;
  private final PortfolioRepository portfolioRepository;
  private final HoldingRepository holdingRepository;

  @Transactional(readOnly = true)
  public List<UniverseDTO> getAllUniverses() {
    log.info("Fetching all active universes");
    return universeRepository.findByIsActiveTrue().stream()
        .map(this::convertToDTO)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public UniverseDTO getUniverse(UUID universeId) {
    log.info("Fetching universe: {}", universeId);
    Universe universe =
        universeRepository
            .findById(universeId)
            .orElseThrow(() -> new NotFoundException("Universe not found"));
    return convertToDTO(universe);
  }

  @Transactional
  public UniverseSelectionResponse selectUniverseForPortfolio(
      UUID portfolioId, UUID universeId) {
    log.info("Selecting universe {} for portfolio {}", universeId, portfolioId);

    Portfolio portfolio =
        portfolioRepository
            .findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));

    Universe universe =
        universeRepository
            .findById(universeId)
            .orElseThrow(() -> new NotFoundException("Universe not found"));

    // Get all holdings for the portfolio
    List<Holding> holdings = holdingRepository.findByPortfolioId(portfolioId);

    // Get universe constituents
    List<UniverseConstituent> constituents =
        universeConstituentRepository.findByUniverseIdAndIsActiveTrue(universeId);
    Set<String> universeSymbols =
        constituents.stream().map(UniverseConstituent::getSymbol).collect(Collectors.toSet());

    // Calculate coverage
    int totalHoldings = holdings.size();
    int holdingsInUniverse = 0;

    if (totalHoldings > 0) {
      for (Holding holding : holdings) {
        if (universeSymbols.contains(holding.getSymbol())) {
          holding.setInUniverse(true);
          holdingsInUniverse++;
        } else {
          holding.setInUniverse(false);
        }
        holdingRepository.save(holding);
      }
    }

    BigDecimal coveragePercentage = BigDecimal.ZERO;
    if (totalHoldings > 0) {
      coveragePercentage =
          BigDecimal.valueOf(holdingsInUniverse)
              .divide(BigDecimal.valueOf(totalHoldings), 4, RoundingMode.HALF_UP)
              .multiply(BigDecimal.valueOf(100));
    }

    // Update portfolio with coverage
    portfolio.setUniverseCoveragePct(coveragePercentage);
    portfolioRepository.save(portfolio);

    log.info(
        "Universe selection complete: {} of {} holdings in universe ({}%)",
        holdingsInUniverse,
        totalHoldings,
        coveragePercentage);

    return UniverseSelectionResponse.builder()
        .selectedUniverseId(universeId)
        .coveragePercentage(coveragePercentage)
        .holdingsInUniverse(holdingsInUniverse)
        .totalHoldings(totalHoldings)
        .build();
  }

  private UniverseDTO convertToDTO(Universe universe) {
    List<String> tickers =
        universeConstituentRepository.findByUniverseIdAndIsActiveTrue(universe.getId()).stream()
            .map(UniverseConstituent::getSymbol)
            .collect(Collectors.toList());

    UniverseDTO dto = UniverseDTO.from(universe);
    dto.setTickerList(tickers);
    return dto;
  }

  public static class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
      super(message);
    }
  }
}
