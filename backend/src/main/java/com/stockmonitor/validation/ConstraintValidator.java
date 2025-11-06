package com.stockmonitor.validation;

import com.stockmonitor.dto.ConstraintSetDTO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validator for constraint values (T141).
 *
 * <p>Validates ranges and logical consistency of constraint values.
 */
@Component
@Slf4j
public class ConstraintValidator {

  /**
   * Validate constraint values.
   *
   * @param dto Constraint set to validate
   * @return Map of field names to error messages, empty if valid
   */
  public Map<String, String> validate(ConstraintSetDTO dto) {
    Map<String, String> errors = new HashMap<>();

    // Validate position sizes (using actual model fields)
    if (dto.getMaxNameWeightLargeCapPct() != null) {
      if (dto.getMaxNameWeightLargeCapPct().compareTo(BigDecimal.ZERO) < 0) {
        errors.put("maxNameWeightLargeCapPct", "Must be >= 0%");
      } else if (dto.getMaxNameWeightLargeCapPct().compareTo(BigDecimal.valueOf(100)) > 0) {
        errors.put("maxNameWeightLargeCapPct", "Must be <= 100%");
      }
    }

    // Validate maxSectorExposurePct (0-100%)
    if (dto.getMaxSectorExposurePct() != null) {
      if (dto.getMaxSectorExposurePct().compareTo(BigDecimal.ZERO) < 0) {
        errors.put("maxSectorExposurePct", "Must be >= 0%");
      } else if (dto.getMaxSectorExposurePct().compareTo(BigDecimal.valueOf(100)) > 0) {
        errors.put("maxSectorExposurePct", "Must be <= 100%");
      }
    }

    // Validate turnoverCapPct (0-100%)
    if (dto.getTurnoverCapPct() != null) {
      if (dto.getTurnoverCapPct().compareTo(BigDecimal.ZERO) < 0) {
        errors.put("turnoverCapPct", "Must be >= 0%");
      } else if (dto.getTurnoverCapPct().compareTo(BigDecimal.valueOf(100)) > 0) {
        errors.put("turnoverCapPct", "Must be <= 100%");
      }
    }

    // Validate liquidityFloorAdvUsd (>= 0)
    if (dto.getLiquidityFloorAdvUsd() != null) {
      if (dto.getLiquidityFloorAdvUsd().compareTo(BigDecimal.ZERO) < 0) {
        errors.put("liquidityFloorAdvUsd", "Must be >= 0");
      }
    }

    // Validate minMarketCapBn (>= 0) - field not in model
    // TODO: Add minMarketCapBn field if needed
    // if (dto.getMinMarketCapBn() != null) {
    //   if (dto.getMinMarketCapBn().compareTo(BigDecimal.ZERO) < 0) {
    //     errors.put("minMarketCapBn", "Must be >= 0");
    //   }
    // }

    // Validate cashBufferPct (0-100%) - field not in current model
    // TODO: Add cashBufferPct to ConstraintSet model if needed
    // if (dto.getCashBufferPct() != null) {
    //   if (dto.getCashBufferPct().compareTo(BigDecimal.ZERO) < 0) {
    //     errors.put("cashBufferPct", "Must be >= 0%");
    //   } else if (dto.getCashBufferPct().compareTo(BigDecimal.valueOf(100)) > 0) {
    //     errors.put("cashBufferPct", "Must be <= 100%");
    //   }
    // }

    // Validate minHoldingPeriodDays (>= 0) - field not in model
    // TODO: Add minHoldingPeriodDays field if needed
    // if (dto.getMinHoldingPeriodDays() != null && dto.getMinHoldingPeriodDays() < 0) {
    //   errors.put("minHoldingPeriodDays", "Must be >= 0");
    // }

    // Validate maxDrawdownThreshold (0-100%) - field not in model
    // TODO: Add maxDrawdownThreshold field if needed
    // if (dto.getMaxDrawdownThreshold() != null) {
    //   if (dto.getMaxDrawdownThreshold().compareTo(BigDecimal.ZERO) < 0) {
    //     errors.put("maxDrawdownThreshold", "Must be >= 0%");
    //   } else if (dto.getMaxDrawdownThreshold().compareTo(BigDecimal.valueOf(100)) > 0) {
    //     errors.put("maxDrawdownThreshold", "Must be <= 100%");
    //   }
    // }

    // Validate maxNumHoldings and minNumHoldings - fields not in model
    // TODO: Add these fields to model if needed
    // if (dto.getMaxNumHoldings() != null && dto.getMinNumHoldings() != null) {
    //   if (dto.getMaxNumHoldings() < dto.getMinNumHoldings()) {
    //     errors.put(
    //         "maxNumHoldings",
    //         String.format(
    //             "Must be >= minNumHoldings (%d)", dto.getMinNumHoldings()));
    //   }
    // }
    //
    // if (dto.getMaxNumHoldings() != null && dto.getMaxNumHoldings() < 1) {
    //   errors.put("maxNumHoldings", "Must be >= 1");
    // }
    //
    // if (dto.getMinNumHoldings() != null && dto.getMinNumHoldings() < 1) {
    //   errors.put("minNumHoldings", "Must be >= 1");
    // }

    // Logical consistency checks
    errors.putAll(validateLogicalConsistency(dto));

    if (!errors.isEmpty()) {
      log.warn("Constraint validation failed with {} errors: {}", errors.size(), errors);
    }

    return errors;
  }

  /**
   * Validate logical consistency between constraints.
   */
  private Map<String, String> validateLogicalConsistency(ConstraintSetDTO dto) {
    Map<String, String> errors = new HashMap<>();

    // Position size should be reasonable given number of holdings
    // Position size vs holdings count validation - fields not in model
    // TODO: Add maxPositionSizePct and minNumHoldings if needed
    // if (dto.getMaxPositionSizePct() != null && dto.getMinNumHoldings() != null) {
    //   BigDecimal impliedMaxPosition =
    //       BigDecimal.valueOf(100.0)
    //           .divide(BigDecimal.valueOf(dto.getMinNumHoldings()), 2, java.math.RoundingMode.HALF_UP);
    //
    //   if (dto.getMaxPositionSizePct().compareTo(impliedMaxPosition) < 0) {
    //     errors.put(
    //         "maxPositionSizePct",
    //         String.format(
    //             "Too small for minNumHoldings (%d). Should be at least %.2f%%",
    //             dto.getMinNumHoldings(), impliedMaxPosition));
    //   }
    // }

    // Sector exposure should be reasonable - validation disabled (fields not in model)
    // TODO: Add maxPositionSizePct field to model if needed
    // if (dto.getMaxSectorExposurePct() != null && dto.getMaxPositionSizePct() != null) {
    //   if (dto.getMaxSectorExposurePct().compareTo(dto.getMaxPositionSizePct()) < 0) {
    //     errors.put(
    //         "maxSectorExposurePct",
    //         "Should be >= maxPositionSizePct (at least one position per sector)");
    //   }
    // }

    // Cash buffer should leave room for positions - field not in current model
    // TODO: Add cashBufferPct validation if field is added to model
    // if (dto.getCashBufferPct() != null) {
    //   if (dto.getCashBufferPct().compareTo(BigDecimal.valueOf(50)) > 0) {
    //     errors.put(
    //         "cashBufferPct",
    //         "Very high cash buffer (>50%) may limit investment capacity");
    //   }
    // }

    return errors;
  }

  /**
   * Validate and throw exception if invalid.
   *
   * @param dto Constraint set to validate
   * @throws ValidationException if validation fails
   */
  public void validateAndThrow(ConstraintSetDTO dto) throws ValidationException {
    Map<String, String> errors = validate(dto);
    if (!errors.isEmpty()) {
      throw new ValidationException("Constraint validation failed", errors);
    }
  }

  /**
   * Custom validation exception with field-level errors.
   */
  public static class ValidationException extends Exception {
    private final Map<String, String> fieldErrors;

    public ValidationException(String message, Map<String, String> fieldErrors) {
      super(message);
      this.fieldErrors = fieldErrors;
    }

    public Map<String, String> getFieldErrors() {
      return fieldErrors;
    }
  }
}
