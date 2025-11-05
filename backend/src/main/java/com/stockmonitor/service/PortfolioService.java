package com.stockmonitor.service;

import com.stockmonitor.dto.HoldingsUploadResponse;
import com.stockmonitor.dto.PortfolioDTO;
import com.stockmonitor.model.Holding;
import com.stockmonitor.model.Portfolio;
import com.stockmonitor.model.User;
import com.stockmonitor.repository.HoldingRepository;
import com.stockmonitor.repository.PortfolioRepository;
import com.stockmonitor.repository.UserRepository;
import com.stockmonitor.service.HoldingsCsvParser.ParseResult;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {

  private final PortfolioRepository portfolioRepository;
  private final HoldingRepository holdingRepository;
  private final UserRepository userRepository;
  private final HoldingsCsvParser csvParser;

  @Transactional
  public PortfolioDTO getOrCreatePortfolio(UUID userId) {
    log.info("Getting or creating portfolio for user: {}", userId);

    Portfolio portfolio =
        portfolioRepository
            .findByUserId(userId)
            .orElseGet(
                () -> {
                  log.info("Creating new portfolio for user: {}", userId);
                  Portfolio newPortfolio =
                      Portfolio.builder()
                          .userId(userId)
                          .build();
                  return portfolioRepository.save(newPortfolio);
                });

    return PortfolioDTO.from(portfolio);
  }

  @Transactional
  public PortfolioDTO getPortfolio(UUID portfolioId) {
    log.info("Getting portfolio: {}", portfolioId);

    Portfolio portfolio =
        portfolioRepository
            .findById(portfolioId)
            .orElseThrow(() -> new PortfolioNotFoundException("Portfolio not found"));

    return PortfolioDTO.from(portfolio);
  }

  @Transactional
  public HoldingsUploadResponse uploadHoldings(UUID portfolioId, InputStream csvFile) {
    log.info("Uploading holdings for portfolio: {}", portfolioId);

    // Get or create portfolio
    Portfolio portfolio =
        portfolioRepository
            .findById(portfolioId)
            .orElseGet(
                () -> {
                  log.info("Creating new portfolio with ID: {}", portfolioId);
                  // Use a fixed UUID for test portfolios without a real user
                  UUID testUserId = UUID.fromString("00000000-0000-0000-0000-000000000099");
                  Portfolio newPortfolio =
                      Portfolio.builder().id(portfolioId).userId(testUserId).build();
                  return portfolioRepository.save(newPortfolio);
                });

    // Parse CSV
    ParseResult parseResult = csvParser.parse(csvFile);

    // If there are validation errors, return them without saving
    if (parseResult.hasErrors()) {
      log.warn("Validation errors found in CSV: {}", parseResult.getErrors().size());
      return HoldingsUploadResponse.builder()
          .totalHoldings(0)
          .marketValue(java.math.BigDecimal.ZERO)
          .uploadedAt(LocalDateTime.now())
          .validationErrors(parseResult.getErrors())
          .build();
    }

    // Clear existing holdings
    holdingRepository.deleteByPortfolioId(portfolioId);
    log.info("Cleared existing holdings for portfolio: {}", portfolioId);

    // Create new holdings
    java.math.BigDecimal totalMarketValue = java.math.BigDecimal.ZERO;

    for (var holdingRequest : parseResult.getHoldings()) {
      java.math.BigDecimal quantity =
          java.math.BigDecimal.valueOf(holdingRequest.getQuantity());
      java.math.BigDecimal costBasisPerShare =
          java.math.BigDecimal.valueOf(holdingRequest.getCostBasis());
      java.math.BigDecimal totalCostBasis = quantity.multiply(costBasisPerShare);

      Holding holding =
          Holding.builder()
              .portfolioId(portfolioId)
              .symbol(holdingRequest.getTicker())
              .quantity(quantity)
              .costBasis(totalCostBasis)
              .costBasisPerShare(costBasisPerShare)
              .currentPrice(costBasisPerShare) // Initially use cost basis as current price
              .currentMarketValue(totalCostBasis)
              .acquisitionDate(LocalDate.now())
              .currency(holdingRequest.getCurrency())
              .build();

      holdingRepository.save(holding);
      totalMarketValue = totalMarketValue.add(holding.getCurrentMarketValue());
    }

    // Update portfolio totals
    portfolio.setTotalMarketValue(totalMarketValue);
    portfolio.setTotalCostBasis(totalMarketValue);
    portfolio.setLastCalculatedAt(LocalDateTime.now());
    portfolioRepository.save(portfolio);

    log.info(
        "Successfully uploaded {} holdings with total market value: {}",
        parseResult.getHoldings().size(),
        totalMarketValue);

    return HoldingsUploadResponse.builder()
        .totalHoldings(parseResult.getHoldings().size())
        .marketValue(totalMarketValue)
        .uploadedAt(LocalDateTime.now())
        .validationErrors(List.of())
        .build();
  }

  public static class PortfolioNotFoundException extends RuntimeException {
    public PortfolioNotFoundException(String message) {
      super(message);
    }
  }

  @Transactional(readOnly = true)
  public List<Holding> getHoldings(UUID portfolioId) {
    log.info("Getting holdings for portfolio: {}", portfolioId);
    return holdingRepository.findByPortfolioId(portfolioId);
  }
}
