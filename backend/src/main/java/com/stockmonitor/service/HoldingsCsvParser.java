package com.stockmonitor.service;

import com.stockmonitor.dto.HoldingsUploadRequest;
import com.stockmonitor.dto.HoldingsUploadResponse.ValidationError;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for parsing CSV files containing portfolio holdings.
 *
 * <p>Expected CSV format: ticker,quantity,cost_basis,currency
 *
 * <p>Validation rules: - ticker: required, non-empty - quantity: required, positive number -
 * cost_basis: required, positive number - currency: required, 3-letter code
 */
@Service
@Slf4j
public class HoldingsCsvParser {

  private static final List<String> REQUIRED_COLUMNS =
      Arrays.asList("ticker", "quantity", "cost_basis", "currency");

  public ParseResult parse(InputStream inputStream) {
    List<HoldingsUploadRequest> holdings = new ArrayList<>();
    List<ValidationError> errors = new ArrayList<>();

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
      String headerLine = reader.readLine();

      if (headerLine == null || headerLine.trim().isEmpty()) {
        throw new IllegalArgumentException("CSV file is empty");
      }

      // Parse header
      List<String> headers = Arrays.asList(headerLine.toLowerCase().split(","));
      validateHeaders(headers);

      int rowNumber = 1; // Start from 1 (header is row 0)
      String line;

      while ((line = reader.readLine()) != null) {
        rowNumber++;

        if (line.trim().isEmpty()) {
          continue; // Skip empty lines
        }

        String[] values = line.split(",", -1); // -1 to include trailing empty strings

        if (values.length != headers.size()) {
          errors.add(
              ValidationError.builder()
                  .row(rowNumber)
                  .column("all")
                  .errorCode("COLUMN_MISMATCH")
                  .message(
                      String.format(
                          "Row %d: Expected %d columns, found %d",
                          rowNumber, headers.size(), values.length))
                  .build());
          continue;
        }

        ValidationResult result = parseRow(values, headers, rowNumber);
        if (result.hasErrors()) {
          errors.addAll(result.getErrors());
        } else {
          holdings.add(result.getHolding());
        }
      }

      log.info("Parsed {} holdings with {} validation errors", holdings.size(), errors.size());

    } catch (IOException e) {
      log.error("Error reading CSV file", e);
      throw new IllegalArgumentException("Failed to read CSV file: " + e.getMessage());
    }

    return new ParseResult(holdings, errors);
  }

  private void validateHeaders(List<String> headers) {
    List<String> missingColumns = new ArrayList<>();

    for (String required : REQUIRED_COLUMNS) {
      if (!headers.contains(required)) {
        missingColumns.add(required);
      }
    }

    if (!missingColumns.isEmpty()) {
      throw new IllegalArgumentException(
          "Missing required columns: " + String.join(", ", missingColumns));
    }
  }

  private ValidationResult parseRow(String[] values, List<String> headers, int rowNumber) {
    List<ValidationError> errors = new ArrayList<>();

    String ticker = getColumnValue(values, headers, "ticker");
    String quantityStr = getColumnValue(values, headers, "quantity");
    String costBasisStr = getColumnValue(values, headers, "cost_basis");
    String currency = getColumnValue(values, headers, "currency");

    // Validate ticker
    if (ticker == null || ticker.trim().isEmpty()) {
      errors.add(
          ValidationError.builder()
              .row(rowNumber)
              .column("ticker")
              .errorCode("MISSING_DATA")
              .message(String.format("Row %d: Ticker is required", rowNumber))
              .build());
    } else {
      // Trim whitespace from ticker
      ticker = ticker.trim();

      // Allow international ticker formats: letters, numbers, dots, hyphens
      // Examples: AAPL, BRK.B, VOD.L, SAP.DE
      if (!ticker.matches("^[A-Z][A-Z0-9.\\-]{0,9}$")) {
        errors.add(
            ValidationError.builder()
                .row(rowNumber)
                .column("ticker")
                .errorCode("INVALID_SYMBOL")
                .message(
                    String.format(
                        "Row %d: Invalid ticker symbol '%s' (must start with letter, 1-10 chars)",
                        rowNumber, ticker))
                .build());
      }
    }

    // Validate quantity
    Double quantity = null;
    if (quantityStr == null || quantityStr.trim().isEmpty()) {
      errors.add(
          ValidationError.builder()
              .row(rowNumber)
              .column("quantity")
              .errorCode("MISSING_DATA")
              .message(String.format("Row %d: Quantity is required", rowNumber))
              .build());
    } else {
      try {
        quantity = Double.parseDouble(quantityStr.trim());
        if (quantity <= 0) {
          errors.add(
              ValidationError.builder()
                  .row(rowNumber)
                  .column("quantity")
                  .errorCode("NEGATIVE_QUANTITY")
                  .message(
                      String.format(
                          "Row %d: Quantity cannot be negative or zero (%.2f shares)",
                          rowNumber, quantity))
                  .build());
        }
      } catch (NumberFormatException e) {
        errors.add(
            ValidationError.builder()
                .row(rowNumber)
                .column("quantity")
                .errorCode("INVALID_NUMBER")
                .message(
                    String.format("Row %d: Invalid quantity '%s'", rowNumber, quantityStr))
                .build());
      }
    }

    // Validate cost basis
    Double costBasis = null;
    if (costBasisStr == null || costBasisStr.trim().isEmpty()) {
      errors.add(
          ValidationError.builder()
              .row(rowNumber)
              .column("cost_basis")
              .errorCode("MISSING_DATA")
              .message(String.format("Row %d: Cost basis is required", rowNumber))
              .build());
    } else {
      try {
        costBasis = Double.parseDouble(costBasisStr.trim());
        if (costBasis <= 0) {
          errors.add(
              ValidationError.builder()
                  .row(rowNumber)
                  .column("cost_basis")
                  .errorCode("INVALID_COST_BASIS")
                  .message(
                      String.format(
                          "Row %d: Cost basis must be positive (%.2f)", rowNumber, costBasis))
                  .build());
        }
      } catch (NumberFormatException e) {
        errors.add(
            ValidationError.builder()
                .row(rowNumber)
                .column("cost_basis")
                .errorCode("INVALID_NUMBER")
                .message(
                    String.format("Row %d: Invalid cost basis '%s'", rowNumber, costBasisStr))
                .build());
      }
    }

    // Validate currency
    if (currency == null || currency.trim().isEmpty()) {
      errors.add(
          ValidationError.builder()
              .row(rowNumber)
              .column("currency")
              .errorCode("MISSING_DATA")
              .message(String.format("Row %d: Currency is required", rowNumber))
              .build());
    } else if (!currency.matches("^[A-Z]{3}$")) {
      errors.add(
          ValidationError.builder()
              .row(rowNumber)
              .column("currency")
              .errorCode("INVALID_CURRENCY")
              .message(
                  String.format(
                      "Row %d: Invalid currency '%s' (must be 3-letter code like USD)",
                      rowNumber, currency))
              .build());
    }

    if (!errors.isEmpty()) {
      return new ValidationResult(null, errors);
    }

    HoldingsUploadRequest holding =
        HoldingsUploadRequest.builder()
            .ticker(ticker.toUpperCase().trim())
            .quantity(quantity)
            .costBasis(costBasis)
            .currency(currency.toUpperCase().trim())
            .build();

    return new ValidationResult(holding, errors);
  }

  private String getColumnValue(String[] values, List<String> headers, String columnName) {
    int index = headers.indexOf(columnName);
    if (index >= 0 && index < values.length) {
      return values[index].trim();
    }
    return null;
  }

  public static class ParseResult {
    private final List<HoldingsUploadRequest> holdings;
    private final List<ValidationError> errors;

    public ParseResult(List<HoldingsUploadRequest> holdings, List<ValidationError> errors) {
      this.holdings = holdings;
      this.errors = errors;
    }

    public List<HoldingsUploadRequest> getHoldings() {
      return holdings;
    }

    public List<ValidationError> getErrors() {
      return errors;
    }

    public boolean hasErrors() {
      return !errors.isEmpty();
    }
  }

  private static class ValidationResult {
    private final HoldingsUploadRequest holding;
    private final List<ValidationError> errors;

    public ValidationResult(HoldingsUploadRequest holding, List<ValidationError> errors) {
      this.holding = holding;
      this.errors = errors;
    }

    public HoldingsUploadRequest getHolding() {
      return holding;
    }

    public List<ValidationError> getErrors() {
      return errors;
    }

    public boolean hasErrors() {
      return !errors.isEmpty();
    }
  }
}
