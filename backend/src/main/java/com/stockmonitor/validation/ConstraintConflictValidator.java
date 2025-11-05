package com.stockmonitor.validation;

import com.stockmonitor.dto.ConstraintSetDTO;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Constraint conflict validator (T224). */
@Component
@Slf4j
public class ConstraintConflictValidator {

  public List<String> validateConflicts(ConstraintSetDTO constraints) {
    List<String> conflicts = new ArrayList<>();

    // TODO: Add maxNumHoldings and minNumHoldings to ConstraintSet model if needed
    // if (constraints.getMaxNumHoldings() != null && constraints.getMinNumHoldings() != null) {
    //   if (constraints.getMaxNumHoldings() < constraints.getMinNumHoldings()) {
    //     conflicts.add("Max holdings must be >= min holdings");
    //   }
    // }

    return conflicts;
  }
}
