package com.company.fxtrading.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Configuration properties for trade validation rules.
 * Loaded from application.yml under fx-trading.validation.
 */
@Component
@ConfigurationProperties(prefix = "fx-trading.validation")
@Data
public class ValidationProperties {

    /**
     * Maximum allowed trade amount (in base currency).
     */
    private BigDecimal maxTradeAmount = new BigDecimal("10000000");

    /**
     * Minimum allowed trade amount (prevents zero or negative amounts).
     */
    private BigDecimal minTradeAmount = new BigDecimal("0.01");

    /**
     * Set of allowed currency codes.
     * Example: EUR,USD,GBP,JPY,CHF,AUD,CAD,NZD,SEK,NOK,DKK
     */
    private Set<String> allowedCurrencies = Set.of(
        "EUR", "USD", "GBP", "JPY", "CHF", "AUD", "CAD", "NZD", "SEK", "NOK", "DKK"
    );

    /**
     * Minimum exchange rate (prevents zero or negative rates).
     */
    private BigDecimal minExchangeRate = new BigDecimal("0.000001");

    /**
     * Maximum exchange rate (sanity check for typos).
     */
    private BigDecimal maxExchangeRate = new BigDecimal("1000000");

    /**
     * Maximum number of days in the future for trade date.
     */
    private int maxFutureDays = 0;

    /**
     * Maximum number of days in the past for trade date.
     */
    private int maxPastDays = 365;

    /**
     * Maximum value date offset from trade date (typically T+2 for spot).
     */
    private int maxValueDateOffset = 7;
}
