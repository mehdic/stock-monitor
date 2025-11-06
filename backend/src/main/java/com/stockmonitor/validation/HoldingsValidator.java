package com.stockmonitor.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validator for holdings upload with specific error codes per FR-006.
 *
 * Error Codes:
 * - INVALID_SYMBOL: Symbol format is invalid or empty
 * - NEGATIVE_QUANTITY: Quantity is negative
 * - MISSING_DATA: Required field is missing
 * - INVALID_CURRENCY: Currency code is not valid ISO 4217
 *
 * Returns validation errors with row number, column, and human-readable message.
 */
public class HoldingsValidator {

    private static final Pattern SYMBOL_PATTERN = Pattern.compile("^[A-Z0-9\\.\\-]{1,10}$");
    private static final int MAX_SYMBOL_LENGTH = 10;

    /**
     * Validate a single holding row from CSV upload.
     *
     * @param symbol Symbol ticker
     * @param quantity Number of shares
     * @param costBasisPerShare Cost basis per share
     * @param currency Currency code
     * @param rowNumber Row number in CSV (for error reporting)
     * @return List of validation errors (empty if valid)
     */
    public List<ValidationError> validateHolding(
            String symbol,
            BigDecimal quantity,
            BigDecimal costBasisPerShare,
            String currency,
            int rowNumber) {

        List<ValidationError> errors = new ArrayList<>();

        // Validate symbol
        if (symbol == null || symbol.trim().isEmpty()) {
            errors.add(ValidationError.builder()
                    .errorCode(ErrorCode.MISSING_DATA)
                    .rowNumber(rowNumber)
                    .column("symbol")
                    .message(String.format("Row %d: Symbol cannot be empty", rowNumber))
                    .build());
        } else {
            String trimmedSymbol = symbol.trim().toUpperCase();
            if (!SYMBOL_PATTERN.matcher(trimmedSymbol).matches()) {
                errors.add(ValidationError.builder()
                        .errorCode(ErrorCode.INVALID_SYMBOL)
                        .rowNumber(rowNumber)
                        .column("symbol")
                        .message(String.format("Row %d: Invalid symbol format '%s'. " +
                                "Symbol must be 1-10 uppercase letters/numbers (A-Z, 0-9, '.', '-')", rowNumber, symbol))
                        .build());
            }
        }

        // Validate quantity
        if (quantity == null) {
            errors.add(ValidationError.builder()
                    .errorCode(ErrorCode.MISSING_DATA)
                    .rowNumber(rowNumber)
                    .column("quantity")
                    .message(String.format("Row %d: Quantity cannot be empty", rowNumber))
                    .build());
        } else if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            errors.add(ValidationError.builder()
                    .errorCode(ErrorCode.NEGATIVE_QUANTITY)
                    .rowNumber(rowNumber)
                    .column("quantity")
                    .message(String.format("Row %d: Quantity cannot be negative (%s shares)", rowNumber, quantity))
                    .build());
        } else if (quantity.compareTo(BigDecimal.ZERO) == 0) {
            errors.add(ValidationError.builder()
                    .errorCode(ErrorCode.INVALID_QUANTITY)
                    .rowNumber(rowNumber)
                    .column("quantity")
                    .message(String.format("Row %d: Quantity cannot be zero", rowNumber))
                    .build());
        }

        // Validate cost basis
        if (costBasisPerShare == null) {
            errors.add(ValidationError.builder()
                    .errorCode(ErrorCode.MISSING_DATA)
                    .rowNumber(rowNumber)
                    .column("cost_basis_per_share")
                    .message(String.format("Row %d: Cost basis per share cannot be empty", rowNumber))
                    .build());
        } else if (costBasisPerShare.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(ValidationError.builder()
                    .errorCode(ErrorCode.INVALID_COST_BASIS)
                    .rowNumber(rowNumber)
                    .column("cost_basis_per_share")
                    .message(String.format("Row %d: Cost basis must be positive (got %s)", rowNumber, costBasisPerShare))
                    .build());
        }

        // Validate currency
        if (currency == null || currency.trim().isEmpty()) {
            errors.add(ValidationError.builder()
                    .errorCode(ErrorCode.MISSING_DATA)
                    .rowNumber(rowNumber)
                    .column("currency")
                    .message(String.format("Row %d: Currency cannot be empty", rowNumber))
                    .build());
        } else {
            try {
                Currency.getInstance(currency.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add(ValidationError.builder()
                        .errorCode(ErrorCode.INVALID_CURRENCY)
                        .rowNumber(rowNumber)
                        .column("currency")
                        .message(String.format("Row %d: Invalid currency code '%s'. Must be valid ISO 4217 code (e.g., USD, EUR, GBP)", rowNumber, currency))
                        .build());
            }
        }

        return errors;
    }

    /**
     * Validate all holdings and return aggregated errors.
     */
    public ValidationResult validateAll(List<HoldingRow> holdings) {
        List<ValidationError> allErrors = new ArrayList<>();

        for (int i = 0; i < holdings.size(); i++) {
            HoldingRow holding = holdings.get(i);
            int rowNumber = i + 2; // +2 because row 1 is header, and index starts at 0

            List<ValidationError> rowErrors = validateHolding(
                    holding.getSymbol(),
                    holding.getQuantity(),
                    holding.getCostBasisPerShare(),
                    holding.getCurrency(),
                    rowNumber
            );

            allErrors.addAll(rowErrors);
        }

        boolean isValid = allErrors.isEmpty();
        return new ValidationResult(isValid, allErrors);
    }

    /**
     * Error codes for holdings validation per FR-006.
     */
    public enum ErrorCode {
        INVALID_SYMBOL,
        NEGATIVE_QUANTITY,
        INVALID_QUANTITY,
        MISSING_DATA,
        INVALID_CURRENCY,
        INVALID_COST_BASIS
    }

    /**
     * Validation error with row number, column, and human-readable message.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        private ErrorCode errorCode;
        private int rowNumber;
        private String column;
        private String message;
    }

    /**
     * Validation result containing all errors.
     */
    @Data
    @AllArgsConstructor
    public static class ValidationResult {
        private boolean valid;
        private List<ValidationError> errors;

        public boolean hasErrors() {
            return !valid;
        }
    }

    /**
     * Holding row data structure for validation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HoldingRow {
        private String symbol;
        private BigDecimal quantity;
        private BigDecimal costBasisPerShare;
        private String currency;
    }
}
