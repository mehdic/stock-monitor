package com.stockmonitor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for multi-currency FX conversion per FR-005.
 *
 * Features:
 * - Convert between currencies at specific dates
 * - Cache FX rates for performance (1 hour TTL)
 * - Fallback to latest rate if historical rate unavailable
 * - Support for major currencies (USD, EUR, GBP, JPY, CHF, CAD, AUD, etc.)
 *
 * Note: This is a stub implementation. In production, integrate with:
 * - ECB API (European Central Bank)
 * - Alpha Vantage FX endpoint
 * - Or other reliable FX data provider
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FxRateService {

    private static final String BASE_CURRENCY = "USD";
    private static final int SCALE = 6; // 6 decimal places for FX rates

    /**
     * Convert amount from source currency to target currency.
     *
     * @param amount Amount to convert
     * @param fromCurrency Source currency code (ISO 4217)
     * @param toCurrency Target currency code (ISO 4217)
     * @param asOfDate Date for FX rate (use latest if null)
     * @return Converted amount in target currency
     */
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency, LocalDate asOfDate) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }

        BigDecimal rate = getRate(fromCurrency, toCurrency, asOfDate);
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Convert amount to portfolio base currency (USD).
     *
     * @param amount Amount to convert
     * @param fromCurrency Source currency code
     * @param asOfDate Date for FX rate
     * @return Amount converted to USD
     */
    public BigDecimal convertToBaseCurrency(BigDecimal amount, String fromCurrency, LocalDate asOfDate) {
        return convert(amount, fromCurrency, BASE_CURRENCY, asOfDate);
    }

    /**
     * Get FX rate from source to target currency.
     * Cached for 1 hour to reduce API calls.
     *
     * @param fromCurrency Source currency
     * @param toCurrency Target currency
     * @param asOfDate Date for rate (null for latest)
     * @return FX rate
     */
    @Cacheable(value = "marketData", key = "#fromCurrency + '-' + #toCurrency + '-' + #asOfDate")
    public BigDecimal getRate(String fromCurrency, String toCurrency, LocalDate asOfDate) {
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }

        // TODO: Integrate with real FX data provider
        // For now, use stub rates
        BigDecimal rate = getStubRate(fromCurrency, toCurrency);

        log.debug("FX rate {}/{} on {}: {}", fromCurrency, toCurrency, asOfDate, rate);
        return rate;
    }

    /**
     * Get latest (spot) FX rate.
     */
    public BigDecimal getLatestRate(String fromCurrency, String toCurrency) {
        return getRate(fromCurrency, toCurrency, LocalDate.now());
    }

    /**
     * Stub FX rates for development/testing.
     * In production, replace with actual API integration.
     */
    private BigDecimal getStubRate(String fromCurrency, String toCurrency) {
        Map<String, BigDecimal> usdRates = getUsdRates();

        if (fromCurrency.equals("USD")) {
            return usdRates.getOrDefault(toCurrency, BigDecimal.ONE);
        } else if (toCurrency.equals("USD")) {
            BigDecimal fromRate = usdRates.getOrDefault(fromCurrency, BigDecimal.ONE);
            return BigDecimal.ONE.divide(fromRate, SCALE, RoundingMode.HALF_UP);
        } else {
            // Cross rate: from -> USD -> to
            BigDecimal fromToUsd = getStubRate(fromCurrency, "USD");
            BigDecimal usdToTarget = getStubRate("USD", toCurrency);
            return fromToUsd.multiply(usdToTarget).setScale(SCALE, RoundingMode.HALF_UP);
        }
    }

    /**
     * Stub USD exchange rates (1 USD = X currency).
     * These are approximate rates for development only.
     */
    private Map<String, BigDecimal> getUsdRates() {
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("USD", BigDecimal.ONE);
        rates.put("EUR", new BigDecimal("0.92"));      // 1 USD = 0.92 EUR
        rates.put("GBP", new BigDecimal("0.79"));      // 1 USD = 0.79 GBP
        rates.put("JPY", new BigDecimal("149.50"));    // 1 USD = 149.50 JPY
        rates.put("CHF", new BigDecimal("0.88"));      // 1 USD = 0.88 CHF
        rates.put("CAD", new BigDecimal("1.36"));      // 1 USD = 1.36 CAD
        rates.put("AUD", new BigDecimal("1.52"));      // 1 USD = 1.52 AUD
        rates.put("NZD", new BigDecimal("1.64"));      // 1 USD = 1.64 NZD
        rates.put("SEK", new BigDecimal("10.45"));     // 1 USD = 10.45 SEK
        rates.put("NOK", new BigDecimal("10.75"));     // 1 USD = 10.75 NOK
        rates.put("DKK", new BigDecimal("6.85"));      // 1 USD = 6.85 DKK
        return rates;
    }

    /**
     * Check if currency is supported.
     */
    public boolean isCurrencySupported(String currencyCode) {
        return getUsdRates().containsKey(currencyCode);
    }
}
