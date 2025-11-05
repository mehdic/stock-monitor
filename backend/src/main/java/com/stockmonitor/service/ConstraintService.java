package com.stockmonitor.service;

import com.stockmonitor.dto.ConstraintSetDTO;
import com.stockmonitor.model.ConstraintSet;
import com.stockmonitor.model.Portfolio;
import com.stockmonitor.repository.ConstraintSetRepository;
import com.stockmonitor.repository.PortfolioRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConstraintService {

  private final ConstraintSetRepository constraintSetRepository;
  private final PortfolioRepository portfolioRepository;

  @Transactional(readOnly = true)
  public ConstraintSetDTO getDefaultConstraints() {
    log.info("Fetching default constraints");
    return ConstraintSetDTO.getDefaults();
  }

  @Transactional(readOnly = true)
  public ConstraintSetDTO getConstraintsForPortfolio(UUID portfolioId) {
    log.info("Fetching constraints for portfolio: {}", portfolioId);

    Portfolio portfolio =
        portfolioRepository
            .findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));

    // Find active constraint set for the user
    Optional<ConstraintSet> activeConstraint =
        constraintSetRepository.findByUserIdAndIsActiveTrue(portfolio.getUserId());

    if (activeConstraint.isEmpty()) {
      log.info("No active constraints found, returning defaults");
      return ConstraintSetDTO.getDefaults();
    }

    return ConstraintSetDTO.from(activeConstraint.get());
  }

  @Transactional
  public ConstraintSetDTO createConstraintSet(UUID userId, ConstraintSetDTO dto) {
    log.info("Creating constraint set for user: {}", userId);

    // Deactivate existing constraint sets for this user
    List<ConstraintSet> existingConstraints = constraintSetRepository.findByUserId(userId);
    for (ConstraintSet existing : existingConstraints) {
      if (existing.getIsActive()) {
        existing.setIsActive(false);
        constraintSetRepository.save(existing);
      }
    }

    // Create new constraint set
    ConstraintSet constraintSet =
        ConstraintSet.builder()
            .userId(userId)
            .name(dto.getName())
            .isActive(true)
            .maxNameWeightLargeCapPct(dto.getMaxNameWeightLargeCapPct())
            .maxNameWeightMidCapPct(dto.getMaxNameWeightMidCapPct())
            .maxNameWeightSmallCapPct(dto.getMaxNameWeightSmallCapPct())
            .maxSectorExposurePct(dto.getMaxSectorExposurePct())
            .turnoverCapPct(dto.getTurnoverCapPct())
            .weightDeadbandBps(dto.getWeightDeadbandBps())
            .participationCapTier1Pct(dto.getParticipationCapTier1Pct())
            .participationCapTier2Pct(dto.getParticipationCapTier2Pct())
            .participationCapTier3Pct(dto.getParticipationCapTier3Pct())
            .participationCapTier4Pct(dto.getParticipationCapTier4Pct())
            .participationCapTier5Pct(dto.getParticipationCapTier5Pct())
            .spreadThresholdBps(dto.getSpreadThresholdBps())
            .earningsBlackoutHours(dto.getEarningsBlackoutHours())
            .liquidityFloorAdvUsd(dto.getLiquidityFloorAdvUsd())
            .costMarginRequiredBps(dto.getCostMarginRequiredBps())
            .version(dto.getVersion() != null ? dto.getVersion() : 1)
            .build();

    ConstraintSet saved = constraintSetRepository.save(constraintSet);
    log.info("Created constraint set with ID: {}", saved.getId());

    return ConstraintSetDTO.from(saved);
  }

  @Transactional
  public ConstraintSetDTO resetToDefaults(UUID userId) {
    log.info("Resetting constraints to defaults for user: {}", userId);

    // Create default constraint set
    ConstraintSetDTO defaults = ConstraintSetDTO.getDefaults();
    return createConstraintSet(userId, defaults);
  }

  /**
   * Update constraints for a portfolio (T139 - FR-016, FR-018, FR-019).
   *
   * <p>Creates new constraint set with incremented version number. Previous versions retained for
   * audit.
   *
   * @param portfolioId Portfolio ID
   * @param dto Updated constraints
   * @return Saved constraint set with new version
   */
  @Transactional
  public ConstraintSet updateConstraints(UUID portfolioId, ConstraintSetDTO dto) {
    log.info("Updating constraints for portfolio: {}", portfolioId);

    Portfolio portfolio =
        portfolioRepository
            .findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));

    // Get current constraint set to increment version
    ConstraintSet currentConstraints = getActiveConstraints(portfolioId);
    int nextVersion = currentConstraints.getVersion() + 1;

    // Deactivate current constraints
    currentConstraints.setIsActive(false);
    constraintSetRepository.save(currentConstraints);

    // Create new constraint set with incremented version
    // Use current constraints as base, then override with DTO values if provided
    ConstraintSet newConstraints =
        ConstraintSet.builder()
            .userId(portfolio.getUserId())
            .name(dto.getName() != null ? dto.getName() : "Custom v" + nextVersion)
            .isActive(true)
            .maxNameWeightLargeCapPct(dto.getMaxNameWeightLargeCapPct() != null ? dto.getMaxNameWeightLargeCapPct() : currentConstraints.getMaxNameWeightLargeCapPct())
            .maxNameWeightMidCapPct(dto.getMaxNameWeightMidCapPct() != null ? dto.getMaxNameWeightMidCapPct() : currentConstraints.getMaxNameWeightMidCapPct())
            .maxNameWeightSmallCapPct(dto.getMaxNameWeightSmallCapPct() != null ? dto.getMaxNameWeightSmallCapPct() : currentConstraints.getMaxNameWeightSmallCapPct())
            .maxSectorExposurePct(dto.getMaxSectorExposurePct() != null ? dto.getMaxSectorExposurePct() : currentConstraints.getMaxSectorExposurePct())
            .turnoverCapPct(dto.getTurnoverCapPct() != null ? dto.getTurnoverCapPct() : currentConstraints.getTurnoverCapPct())
            .weightDeadbandBps(dto.getWeightDeadbandBps() != null ? dto.getWeightDeadbandBps() : currentConstraints.getWeightDeadbandBps())
            .participationCapTier1Pct(dto.getParticipationCapTier1Pct() != null ? dto.getParticipationCapTier1Pct() : currentConstraints.getParticipationCapTier1Pct())
            .participationCapTier2Pct(dto.getParticipationCapTier2Pct() != null ? dto.getParticipationCapTier2Pct() : currentConstraints.getParticipationCapTier2Pct())
            .participationCapTier3Pct(dto.getParticipationCapTier3Pct() != null ? dto.getParticipationCapTier3Pct() : currentConstraints.getParticipationCapTier3Pct())
            .participationCapTier4Pct(dto.getParticipationCapTier4Pct() != null ? dto.getParticipationCapTier4Pct() : currentConstraints.getParticipationCapTier4Pct())
            .participationCapTier5Pct(dto.getParticipationCapTier5Pct() != null ? dto.getParticipationCapTier5Pct() : currentConstraints.getParticipationCapTier5Pct())
            .liquidityFloorAdvUsd(dto.getLiquidityFloorAdvUsd() != null ? dto.getLiquidityFloorAdvUsd() : currentConstraints.getLiquidityFloorAdvUsd())
            .spreadThresholdBps(dto.getSpreadThresholdBps() != null ? dto.getSpreadThresholdBps() : currentConstraints.getSpreadThresholdBps())
            .earningsBlackoutHours(dto.getEarningsBlackoutHours() != null ? dto.getEarningsBlackoutHours() : currentConstraints.getEarningsBlackoutHours())
            .costMarginRequiredBps(dto.getCostMarginRequiredBps() != null ? dto.getCostMarginRequiredBps() : currentConstraints.getCostMarginRequiredBps())
            .version(nextVersion)
            .build();

    ConstraintSet saved = constraintSetRepository.save(newConstraints);

    // Update portfolio to point to new constraint set
    portfolio.setActiveConstraintSetId(saved.getId());
    portfolioRepository.save(portfolio);

    log.info(
        "Updated constraints for portfolio {} to version {}", portfolioId, saved.getVersion());
    return saved;
  }

  /**
   * Get active constraint set for a portfolio.
   *
   * @param portfolioId Portfolio ID
   * @return Active constraint set
   * @throws IllegalStateException if no active constraints found
   */
  @Transactional(readOnly = true)
  public ConstraintSet getActiveConstraints(UUID portfolioId) {
    Portfolio portfolio =
        portfolioRepository
            .findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));

    if (portfolio.getActiveConstraintSetId() == null) {
      throw new IllegalStateException("No active constraint set configured for portfolio");
    }

    return constraintSetRepository
        .findById(portfolio.getActiveConstraintSetId())
        .orElseThrow(() -> new IllegalStateException("Active constraint set not found"));
  }

  /**
   * Reset constraints to defaults for a portfolio (FR-018).
   *
   * @param portfolioId Portfolio ID
   * @return New constraint set with default values
   */
  @Transactional
  public ConstraintSet resetPortfolioConstraintsToDefaults(UUID portfolioId) {
    log.info("Resetting constraints to defaults for portfolio: {}", portfolioId);

    Portfolio portfolio =
        portfolioRepository
            .findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));

    // Get current version
    int nextVersion = 1;
    try {
      ConstraintSet current = getActiveConstraints(portfolioId);
      nextVersion = current.getVersion() + 1;
      current.setIsActive(false);
      constraintSetRepository.save(current);
    } catch (IllegalStateException e) {
      // No current constraints, start at version 1
    }

    // Create default constraint set
    ConstraintSetDTO defaults = ConstraintSetDTO.getDefaults();

    ConstraintSet newConstraints =
        ConstraintSet.builder()
            .userId(portfolio.getUserId())
            .name("Default v" + nextVersion)
            .isActive(true)
            .maxNameWeightLargeCapPct(defaults.getMaxNameWeightLargeCapPct())
            .maxNameWeightMidCapPct(defaults.getMaxNameWeightMidCapPct())
            .maxNameWeightSmallCapPct(defaults.getMaxNameWeightSmallCapPct())
            .maxSectorExposurePct(defaults.getMaxSectorExposurePct())
            .turnoverCapPct(defaults.getTurnoverCapPct())
            .weightDeadbandBps(defaults.getWeightDeadbandBps())
            .participationCapTier1Pct(defaults.getParticipationCapTier1Pct())
            .participationCapTier2Pct(defaults.getParticipationCapTier2Pct())
            .participationCapTier3Pct(defaults.getParticipationCapTier3Pct())
            .participationCapTier4Pct(defaults.getParticipationCapTier4Pct())
            .participationCapTier5Pct(defaults.getParticipationCapTier5Pct())
            .spreadThresholdBps(defaults.getSpreadThresholdBps())
            .earningsBlackoutHours(defaults.getEarningsBlackoutHours())
            .liquidityFloorAdvUsd(defaults.getLiquidityFloorAdvUsd())
            .costMarginRequiredBps(defaults.getCostMarginRequiredBps())
            .version(nextVersion)
            .build();

    ConstraintSet saved = constraintSetRepository.save(newConstraints);

    // Update portfolio
    portfolio.setActiveConstraintSetId(saved.getId());
    portfolioRepository.save(portfolio);

    return saved;
  }
}
