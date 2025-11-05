package com.stockmonitor.service;

import com.stockmonitor.dto.FactorScoreDTO;
import com.stockmonitor.engine.FactorCalculationService;
import com.stockmonitor.model.Holding;
import com.stockmonitor.repository.HoldingRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for factor analysis and heatmap generation (T153, FR-034 to FR-036).
 *
 * <p>Provides: - Factor score calculation with sector normalization (T155) - Heatmap generation
 * for portfolio holdings - Percentile ranking within sector
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FactorService {

  private final HoldingRepository holdingRepository;
  private final FactorCalculationService factorCalculationService;

  /**
   * Get factor scores for all holdings in a portfolio (FR-034).
   *
   * @param portfolioId Portfolio UUID
   * @return List of factor scores, sector-normalized
   */
  @Transactional(readOnly = true)
  public List<FactorScoreDTO> getPortfolioFactors(UUID portfolioId) {
    log.info("Getting factor scores for portfolio: {}", portfolioId);

    List<Holding> holdings = holdingRepository.findByPortfolioId(portfolioId);

    if (holdings.isEmpty()) {
      log.warn("No holdings found for portfolio: {}", portfolioId);
      return Collections.emptyList();
    }

    // Group holdings by sector for normalization
    Map<String, List<Holding>> holdingsBySector =
        holdings.stream().collect(Collectors.groupingBy(Holding::getSector));

    List<FactorScoreDTO> allScores = new ArrayList<>();

    // Calculate and normalize scores sector by sector
    for (Map.Entry<String, List<Holding>> entry : holdingsBySector.entrySet()) {
      String sector = entry.getKey();
      List<Holding> sectorHoldings = entry.getValue();

      List<FactorScoreDTO> sectorScores = calculateAndNormalizeSectorScores(sector,
       sectorHoldings);
      allScores.addAll(sectorScores);
    }

    return allScores;
  }

  /**
   * Get factor scores for a specific holding with detailed breakdown (FR-034, FR-036).
   *
   * @param holdingId Holding UUID
   * @return Factor scores with raw scores and percentiles
   */
  @Transactional(readOnly = true)
  public FactorScoreDTO getHoldingFactors(UUID holdingId) {
    log.info("Getting factor scores for holding: {}", holdingId);

    Holding holding =
        holdingRepository
            .findById(holdingId)
            .orElseThrow(() -> new IllegalArgumentException("Holding not found: " + holdingId));

    // Get all holdings in same sector for normalization
    List<Holding> sectorHoldings =
        holdingRepository.findByPortfolioIdAndSector(holding.getPortfolioId(),
         holding.getSector());

    // Calculate raw scores
    FactorScoreDTO rawScores = factorCalculationService.calculateFactorScores(holding);

    // Normalize and add percentiles
    FactorScoreDTO normalizedScores =
        normalizeScoresWithinSector(holding, rawScores, sectorHoldings);

    return normalizedScores;
  }

  /**
   * Calculate and normalize factor scores for all holdings in a sector (T155, FR-036).
   *
   * <p>Sector normalization converts raw scores to z-scores: z = (raw - mean) / std_dev
   *
   * @param sector Sector name
   * @param holdings Holdings in sector
   * @return Sector-normalized factor scores
   */
  private List<FactorScoreDTO> calculateAndNormalizeSectorScores(
      String sector, List<Holding> holdings) {
    log.debug("Calculating factor scores for {} holdings in sector: {}", holdings.size(), sector);

    // Step 1: Calculate raw scores for all holdings
    List<FactorScoreDTO> rawScores =
        holdings.stream()
            .map(factorCalculationService::calculateFactorScores)
            .collect(Collectors.toList());

    // Step 2: Calculate sector statistics (mean, std dev) for each factor
    SectorStatistics stats = calculateSectorStatistics(rawScores);

    // Step 3: Normalize each holding's scores to z-scores
    List<FactorScoreDTO> normalizedScores = new ArrayList<>();
    for (int i = 0; i < holdings.size(); i++) {
      Holding holding = holdings.get(i);
      FactorScoreDTO raw = rawScores.get(i);

      FactorScoreDTO normalized =
          FactorScoreDTO.builder()
              .symbol(holding.getSymbol())
              .sector(sector)
              .calculatedAt(LocalDateTime.now())
              // Normalized z-scores
              .value(calculateZScore(raw.getValue(), stats.valueMean, stats.valueStdDev))
              .momentum(calculateZScore(raw.getMomentum(), stats.momentumMean,
               stats.momentumStdDev))
              .quality(calculateZScore(raw.getQuality(), stats.qualityMean, stats.qualityStdDev))
              .revisions(
                  calculateZScore(raw.getRevisions(), stats.revisionsMean, stats.revisionsStdDev))
              // Raw scores for reference
              .rawValue(raw.getValue())
              .rawMomentum(raw.getMomentum())
              .rawQuality(raw.getQuality())
              .rawRevisions(raw.getRevisions())
              .build();

      // Calculate composite score
      normalized.setComposite(
          calculateComposite(
              normalized.getValue(),
              normalized.getMomentum(),
              normalized.getQuality(),
              normalized.getRevisions()));

      normalizedScores.add(normalized);
    }

    return normalizedScores;
  }

  /**
   * Normalize scores for single holding within sector peers.
   *
   * @param holding Holding to normalize
   * @param rawScores Raw factor scores
   * @param sectorHoldings All holdings in sector for normalization
   * @return Normalized scores with percentiles
   */
  private FactorScoreDTO normalizeScoresWithinSector(
      Holding holding, FactorScoreDTO rawScores, List<Holding> sectorHoldings) {
    // Calculate raw scores for all sector peers
    List<FactorScoreDTO> allRawScores =
        sectorHoldings.stream()
            .map(factorCalculationService::calculateFactorScores)
            .collect(Collectors.toList());

    SectorStatistics stats = calculateSectorStatistics(allRawScores);

    // Normalize and calculate percentiles
    FactorScoreDTO normalized =
        FactorScoreDTO.builder()
            .symbol(holding.getSymbol())
            .sector(holding.getSector())
            .calculatedAt(LocalDateTime.now())
            // Normalized z-scores
            .value(calculateZScore(rawScores.getValue(), stats.valueMean, stats.valueStdDev))
            .momentum(
                calculateZScore(rawScores.getMomentum(), stats.momentumMean,
                 stats.momentumStdDev))
            .quality(calculateZScore(rawScores.getQuality(), stats.qualityMean,
             stats.qualityStdDev))
            .revisions(
                calculateZScore(rawScores.getRevisions(), stats.revisionsMean,
                 stats.revisionsStdDev))
            // Raw scores
            .rawValue(rawScores.getValue())
            .rawMomentum(rawScores.getMomentum())
            .rawQuality(rawScores.getQuality())
            .rawRevisions(rawScores.getRevisions())
            // Percentiles
            .valuePercentile(calculatePercentile(rawScores.getValue(), allRawScores, "value"))
            .momentumPercentile(
                calculatePercentile(rawScores.getMomentum(), allRawScores, "momentum"))
            .qualityPercentile(calculatePercentile(rawScores.getQuality(), allRawScores,
             "quality"))
            .revisionsPercentile(
                calculatePercentile(rawScores.getRevisions(), allRawScores, "revisions"))
            .build();

    normalized.setComposite(
        calculateComposite(
            normalized.getValue(),
            normalized.getMomentum(),
            normalized.getQuality(),
            normalized.getRevisions()));

    return normalized;
  }

  /**
   * Calculate z-score: (value - mean) / std_dev.
   *
   * @param value Raw value
   * @param mean Sector mean
   * @param stdDev Sector standard deviation
   * @return Z-score
   */
  private BigDecimal calculateZScore(BigDecimal value, BigDecimal mean, BigDecimal stdDev) {
    if (stdDev.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO; // Avoid division by zero
    }
    return value.subtract(mean).divide(stdDev, 2, java.math.RoundingMode.HALF_UP);
  }

  /**
   * Calculate composite factor score (weighted average).
   *
   * @param value Value z-score
   * @param momentum Momentum z-score
   * @param quality Quality z-score
   * @param revisions Revisions z-score
   * @return Composite z-score
   */
  private BigDecimal calculateComposite(
      BigDecimal value, BigDecimal momentum, BigDecimal quality, BigDecimal revisions) {
    // Equal weights: 25% each
    return value
        .add(momentum)
        .add(quality)
        .add(revisions)
        .divide(BigDecimal.valueOf(4), 2, java.math.RoundingMode.HALF_UP);
  }

  /**
   * Calculate percentile rank within sector (0-100).
   *
   * @param value Value to rank
   * @param allScores All sector scores
   * @param factorName Factor name to extract
   * @return Percentile (0-100)
   */
  private Integer calculatePercentile(
      BigDecimal value, List<FactorScoreDTO> allScores, String factorName) {
    List<BigDecimal> values =
        allScores.stream()
            .map(
                s -> {
                  switch (factorName) {
                    case "value":
                      return s.getValue();
                    case "momentum":
                      return s.getMomentum();
                    case "quality":
                      return s.getQuality();
                    case "revisions":
                      return s.getRevisions();
                    default:
                      return BigDecimal.ZERO;
                  }
                })
            .sorted()
            .collect(Collectors.toList());

    int rank = values.indexOf(value);
    return (int) ((double) rank / values.size() * 100);
  }

  /**
   * Calculate sector statistics for normalization.
   *
   * @param scores Raw factor scores for sector
   * @return Sector statistics (mean, std dev)
   */
  private SectorStatistics calculateSectorStatistics(List<FactorScoreDTO> scores) {
    int n = scores.size();
    if (n == 0) {
      return new SectorStatistics();
    }

    // Calculate means
    BigDecimal valueMean =
        scores.stream()
            .map(FactorScoreDTO::getValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(n), 2, java.math.RoundingMode.HALF_UP);

    BigDecimal momentumMean =
        scores.stream()
            .map(FactorScoreDTO::getMomentum)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(n), 2, java.math.RoundingMode.HALF_UP);

    BigDecimal qualityMean =
        scores.stream()
            .map(FactorScoreDTO::getQuality)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(n), 2, java.math.RoundingMode.HALF_UP);

    BigDecimal revisionsMean =
        scores.stream()
            .map(FactorScoreDTO::getRevisions)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(n), 2, java.math.RoundingMode.HALF_UP);

    // Calculate standard deviations
    BigDecimal valueStdDev = calculateStdDev(scores, FactorScoreDTO::getValue, valueMean);
    BigDecimal momentumStdDev = calculateStdDev(scores, FactorScoreDTO::getMomentum, momentumMean);
    BigDecimal qualityStdDev = calculateStdDev(scores, FactorScoreDTO::getQuality, qualityMean);
    BigDecimal revisionsStdDev =
        calculateStdDev(scores, FactorScoreDTO::getRevisions, revisionsMean);

    return new SectorStatistics(
        valueMean,
        valueStdDev,
        momentumMean,
        momentumStdDev,
        qualityMean,
        qualityStdDev,
        revisionsMean,
        revisionsStdDev);
  }

  /**
   * Calculate standard deviation.
   *
   * @param scores Factor scores
   * @param extractor Function to extract factor value
   * @param mean Mean value
   * @return Standard deviation
   */
  private BigDecimal calculateStdDev(
      List<FactorScoreDTO> scores,
      java.util.function.Function<FactorScoreDTO, BigDecimal> extractor,
      BigDecimal mean) {
    int n = scores.size();
    if (n <= 1) {
      return BigDecimal.ONE; // Avoid division by zero
    }

    BigDecimal variance =
        scores.stream()
            .map(extractor)
            .map(v -> v.subtract(mean).pow(2))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(n - 1), 2, java.math.RoundingMode.HALF_UP);

    return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
  }

  /** Sector statistics for normalization. */
  private static class SectorStatistics {
    BigDecimal valueMean;
    BigDecimal valueStdDev;
    BigDecimal momentumMean;
    BigDecimal momentumStdDev;
    BigDecimal qualityMean;
    BigDecimal qualityStdDev;
    BigDecimal revisionsMean;
    BigDecimal revisionsStdDev;

    SectorStatistics() {
      this.valueMean = BigDecimal.ZERO;
      this.valueStdDev = BigDecimal.ONE;
      this.momentumMean = BigDecimal.ZERO;
      this.momentumStdDev = BigDecimal.ONE;
      this.qualityMean = BigDecimal.ZERO;
      this.qualityStdDev = BigDecimal.ONE;
      this.revisionsMean = BigDecimal.ZERO;
      this.revisionsStdDev = BigDecimal.ONE;
    }

    SectorStatistics(
        BigDecimal valueMean,
        BigDecimal valueStdDev,
        BigDecimal momentumMean,
        BigDecimal momentumStdDev,
        BigDecimal qualityMean,
        BigDecimal qualityStdDev,
        BigDecimal revisionsMean,
        BigDecimal revisionsStdDev) {
      this.valueMean = valueMean;
      this.valueStdDev = valueStdDev;
      this.momentumMean = momentumMean;
      this.momentumStdDev = momentumStdDev;
      this.qualityMean = qualityMean;
      this.qualityStdDev = qualityStdDev;
      this.revisionsMean = revisionsMean;
      this.revisionsStdDev = revisionsStdDev;
    }
  }
}
