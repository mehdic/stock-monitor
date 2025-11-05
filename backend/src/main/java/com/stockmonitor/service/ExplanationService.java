package com.stockmonitor.service;

import com.stockmonitor.model.FactorScore;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for generating human-readable explanations for recommendations.
 *
 * <p>Creates explanations based on factor scores, constraint violations, and ranking drivers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExplanationService {

  /**
   * Generate explanation for a recommendation.
   *
   * @param symbol Stock symbol
   * @param rank Recommendation rank (1 = top)
   * @param factorScores Map of factor type to score
   * @param topDrivers Top 3 factor drivers with their scores
   * @param constraintNotes Any constraint notes/warnings
   * @return Human-readable explanation
   */
  public String generateExplanation(
      String symbol,
      int rank,
      Map<String, FactorScore> factorScores,
      List<FactorDriver> topDrivers,
      String constraintNotes) {

    StringBuilder explanation = new StringBuilder();

    // Rank context
    explanation.append(String.format("Ranked #%d. ", rank));

    // Top drivers explanation
    if (!topDrivers.isEmpty()) {
      explanation.append("Primary drivers: ");
      List<String> driverDescriptions = new ArrayList<>();

      for (FactorDriver driver : topDrivers.subList(0, Math.min(3, topDrivers.size()))) {
        String strength = getStrengthDescription(driver.score());
        driverDescriptions.add(
            String.format("%s %s (%.2f)", strength, driver.factorName(), driver.score()));
      }

      explanation.append(String.join(", ", driverDescriptions));
      explanation.append(". ");
    }

    // Factor percentile context
    if (!factorScores.isEmpty()) {
      List<String> percentileNotes = new ArrayList<>();

      for (Map.Entry<String, FactorScore> entry : factorScores.entrySet()) {
        FactorScore score = entry.getValue();
        BigDecimal percentile = score.getPercentileRankUniverse();

        if (percentile.compareTo(BigDecimal.valueOf(80)) > 0) {
          percentileNotes.add(
              String.format(
                  "%s in top %.0f%% of universe",
                  entry.getKey(), BigDecimal.valueOf(100).subtract(percentile)));
        }
      }

      if (!percentileNotes.isEmpty()) {
        explanation.append(String.join(", ", percentileNotes));
        explanation.append(". ");
      }
    }

    // Constraint notes
    if (constraintNotes != null && !constraintNotes.isEmpty()) {
      explanation.append("Note: ").append(constraintNotes);
    }

    return explanation.toString().trim();
  }

  /**
   * Identify top factor drivers from factor scores.
   *
   * @param factorScores Map of factor type to FactorScore
   * @return List of top 3 drivers sorted by strength
   */
  public List<FactorDriver> identifyTopDrivers(Map<String, FactorScore> factorScores) {
    return factorScores.entrySet().stream()
        .map(
            entry -> {
              String factorName = formatFactorName(entry.getKey());
              BigDecimal score = entry.getValue().getSectorNormalizedScore();
              return new FactorDriver(factorName, score);
            })
        .sorted(Comparator.comparing(FactorDriver::score).reversed())
        .limit(3)
        .collect(Collectors.toList());
  }

  private String getStrengthDescription(BigDecimal score) {
    double scoreValue = score.doubleValue();

    if (scoreValue > 2.0) {
      return "Very strong";
    } else if (scoreValue > 1.0) {
      return "Strong";
    } else if (scoreValue > 0.5) {
      return "Moderate";
    } else if (scoreValue > -0.5) {
      return "Neutral";
    } else if (scoreValue > -1.0) {
      return "Weak";
    } else {
      return "Very weak";
    }
  }

  private String formatFactorName(String factorType) {
    return switch (factorType.toUpperCase()) {
      case "VALUE" -> "Value";
      case "MOMENTUM" -> "Momentum";
      case "QUALITY" -> "Quality";
      case "SIZE" -> "Size";
      case "VOLATILITY" -> "Low Volatility";
      default -> factorType;
    };
  }

  /** Factor driver record */
  public record FactorDriver(String factorName, BigDecimal score) {}
}
