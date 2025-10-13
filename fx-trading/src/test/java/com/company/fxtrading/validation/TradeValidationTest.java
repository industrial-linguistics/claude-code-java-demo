package com.company.fxtrading.validation;

import com.company.fxtrading.config.ValidationProperties;
import com.company.fxtrading.domain.Direction;
import com.company.fxtrading.domain.Trade;
import com.company.fxtrading.domain.TradeStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Trade entity validation annotations and custom validators.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("unit")
class TradeValidationTest {

    @Autowired
    private Validator validator;

    @Autowired
    private ValidationProperties validationProperties;

    private Trade validTrade;

    @BeforeEach
    void setUp() {
        // Create a valid trade as baseline
        validTrade = new Trade();
        validTrade.setTradeDate(LocalDate.now());
        validTrade.setValueDate(LocalDate.now().plusDays(2));
        validTrade.setDirection(Direction.BUY);
        validTrade.setBaseCurrency("EUR");
        validTrade.setQuoteCurrency("USD");
        validTrade.setBaseAmount(new BigDecimal("1000000.00"));
        validTrade.setExchangeRate(new BigDecimal("1.0850"));
        validTrade.setQuoteAmount(new BigDecimal("1085000.0000"));
        validTrade.setStatus(TradeStatus.PENDING);
        validTrade.setTrader("Test Trader");
        validTrade.setCreatedBy("test");
        validTrade.setUpdatedBy("test");
    }

    @Test
    void validTrade_ShouldPassValidation() {
        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

        // Then
        assertThat(violations).isEmpty();
    }

    // === Required Field Tests ===

    @Test
    void nullTradeDate_ShouldFailValidation() {
        // Given
        validTrade.setTradeDate(null);

        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations).anyMatch(v ->
            v.getPropertyPath().toString().equals("tradeDate") &&
            v.getMessage().contains("Trade date is required")
        );
    }

    @Test
    void nullValueDate_ShouldFailValidation() {
        // Given
        validTrade.setValueDate(null);

        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations).anyMatch(v ->
            v.getPropertyPath().toString().equals("valueDate") &&
            v.getMessage().contains("Value date is required")
        );
    }

    @Test
    void nullDirection_ShouldFailValidation() {
        // Given
        validTrade.setDirection(null);

        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations).anyMatch(v ->
            v.getPropertyPath().toString().equals("direction")
        );
    }

    @Test
    void nullBaseAmount_ShouldFailValidation() {
        // Given
        validTrade.setBaseAmount(null);

        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations).anyMatch(v ->
            v.getPropertyPath().toString().equals("baseAmount")
        );
    }

    @Test
    void nullExchangeRate_ShouldFailValidation() {
        // Given
        validTrade.setExchangeRate(null);

        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations).anyMatch(v ->
            v.getPropertyPath().toString().equals("exchangeRate")
        );
    }

    // === Currency Validation Tests ===

    @Test
    void invalidBaseCurrency_ShouldFailValidation() {
        // Given
        validTrade.setBaseCurrency("XXX");

        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations).anyMatch(v ->
            v.getPropertyPath().toString().equals("baseCurrency") &&
            v.getMessage().contains("Invalid currency code")
        );
    }

    @Test
    void invalidQuoteCurrency_ShouldFailValidation() {
        // Given
        validTrade.setQuoteCurrency("ZZZ");

        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations).anyMatch(v ->
            v.getPropertyPath().toString().equals("quoteCurrency") &&
            v.getMessage().contains("Invalid currency code")
        );
    }

    @Test
    void allConfiguredCurrencies_ShouldBeValid() {
        // Test each configured currency
        for (String currency : validationProperties.getAllowedCurrencies()) {
            validTrade.setBaseCurrency(currency);
            validTrade.setQuoteCurrency("USD");

            Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

            assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("baseCurrency"))
                .isEmpty();
        }
    }

    // === Amount Validation Tests ===

    @Test
    void zeroBaseAmount_ShouldFailValidation() {
        // Given
        validTrade.setBaseAmount(BigDecimal.ZERO);

        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations).anyMatch(v ->
            v.getPropertyPath().toString().equals("baseAmount") &&
            v.getMessage().contains("at least 0.01")
        );
    }

    @Test
    void negativeBaseAmount_ShouldFailValidation() {
        // Given
        validTrade.setBaseAmount(new BigDecimal("-100"));

        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations).anyMatch(v ->
            v.getPropertyPath().toString().equals("baseAmount")
        );
    }

    @Test
    void baseAmountExceedsMaximum_ShouldFailValidation() {
        // Given
        validTrade.setBaseAmount(new BigDecimal("100000000"));

        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations).anyMatch(v ->
            v.getPropertyPath().toString().equals("baseAmount") &&
            v.getMessage().contains("cannot exceed 10,000,000")
        );
    }

    @Test
    void minimumValidBaseAmount_ShouldPassValidation() {
        // Given
        validTrade.setBaseAmount(new BigDecimal("0.01"));

        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

        // Then
        assertThat(violations)
            .filteredOn(v -> v.getPropertyPath().toString().equals("baseAmount"))
            .isEmpty();
    }

    @Test
    void maximumValidBaseAmount_ShouldPassValidation() {
        // Given
        validTrade.setBaseAmount(new BigDecimal("10000000"));

        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

        // Then
        assertThat(violations)
            .filteredOn(v -> v.getPropertyPath().toString().equals("baseAmount"))
            .isEmpty();
    }

    // === Exchange Rate Validation Tests ===

    @Test
    void zeroExchangeRate_ShouldFailValidation() {
        // Given
        validTrade.setExchangeRate(BigDecimal.ZERO);

        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations).anyMatch(v ->
            v.getPropertyPath().toString().equals("exchangeRate") &&
            v.getMessage().contains("must be positive")
        );
    }

    @Test
    void negativeExchangeRate_ShouldFailValidation() {
        // Given
        validTrade.setExchangeRate(new BigDecimal("-1.5"));

        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations).anyMatch(v ->
            v.getPropertyPath().toString().equals("exchangeRate")
        );
    }

    @Test
    void exchangeRateExceedsMaximum_ShouldFailValidation() {
        // Given
        validTrade.setExchangeRate(new BigDecimal("2000000"));

        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations).anyMatch(v ->
            v.getPropertyPath().toString().equals("exchangeRate") &&
            v.getMessage().contains("cannot exceed 1,000,000")
        );
    }

    // === Date Validation Tests ===

    @Test
    void valueDateBeforeTradeDate_ShouldFailValidation() {
        // Given
        validTrade.setTradeDate(LocalDate.now());
        validTrade.setValueDate(LocalDate.now().minusDays(1));

        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations).anyMatch(v ->
            v.getPropertyPath().toString().equals("valueDate") &&
            v.getMessage().contains("on or after trade date")
        );
    }

    @Test
    void tradeDateTooFarInPast_ShouldFailValidation() {
        // Given - 400 days in the past (exceeds 365 day limit)
        validTrade.setTradeDate(LocalDate.now().minusDays(400));
        validTrade.setValueDate(LocalDate.now().minusDays(398));

        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations).anyMatch(v ->
            v.getPropertyPath().toString().equals("tradeDate") &&
            v.getMessage().contains("cannot be more than")
        );
    }

    @Test
    void valueDateTooFarFromTradeDate_ShouldFailValidation() {
        // Given - 10 days offset (exceeds 7 day limit)
        validTrade.setTradeDate(LocalDate.now());
        validTrade.setValueDate(LocalDate.now().plusDays(10));

        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations).anyMatch(v ->
            v.getPropertyPath().toString().equals("valueDate") &&
            v.getMessage().contains("cannot be more than")
        );
    }

    @Test
    void sameTradeAndValueDate_ShouldPassValidation() {
        // Given
        LocalDate today = LocalDate.now();
        validTrade.setTradeDate(today);
        validTrade.setValueDate(today);

        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

        // Then
        assertThat(violations)
            .filteredOn(v -> v.getPropertyPath().toString().contains("Date"))
            .isEmpty();
    }

    // === String Length Validation Tests ===

    @Test
    void counterpartyTooLong_ShouldFailValidation() {
        // Given - 101 characters
        validTrade.setCounterparty("A".repeat(101));

        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations).anyMatch(v ->
            v.getPropertyPath().toString().equals("counterparty") &&
            v.getMessage().contains("cannot exceed 100 characters")
        );
    }

    @Test
    void traderNameTooLong_ShouldFailValidation() {
        // Given - 51 characters
        validTrade.setTrader("A".repeat(51));

        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations).anyMatch(v ->
            v.getPropertyPath().toString().equals("trader") &&
            v.getMessage().contains("cannot exceed 50 characters")
        );
    }

    @Test
    void notesTooLong_ShouldFailValidation() {
        // Given - 501 characters
        validTrade.setNotes("A".repeat(501));

        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(validTrade);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations).anyMatch(v ->
            v.getPropertyPath().toString().equals("notes") &&
            v.getMessage().contains("cannot exceed 500 characters")
        );
    }

    // === Multiple Errors Test ===

    @Test
    void multipleValidationErrors_ShouldReturnAllViolations() {
        // Given - Invalid trade with multiple errors
        Trade invalidTrade = new Trade();
        invalidTrade.setTradeDate(null);
        invalidTrade.setValueDate(null);
        invalidTrade.setBaseCurrency("XXX");
        invalidTrade.setBaseAmount(BigDecimal.ZERO);

        // When
        Set<ConstraintViolation<Trade>> violations = validator.validate(invalidTrade);

        // Then - Should have at least 4 violations
        assertThat(violations).hasSizeGreaterThanOrEqualTo(4);
    }
}
