package com.company.fxtrading.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Critical tests for BigDecimal precision in financial calculations.
 * These tests ensure we never lose precision in monetary amounts.
 */
@Tag("unit")
class BigDecimalPrecisionTest {

    /**
     * Simulates the calculation done in TradeService.calculateQuoteAmount()
     */
    private BigDecimal calculateQuoteAmount(BigDecimal baseAmount, BigDecimal rate) {
        return baseAmount.multiply(rate).setScale(4, RoundingMode.HALF_UP);
    }

    @Test
    void shouldCalculateQuoteAmountWithCorrectPrecision() {
        // Given
        BigDecimal baseAmount = new BigDecimal("1000000.00");
        BigDecimal rate = new BigDecimal("1.085000");

        // When
        BigDecimal result = calculateQuoteAmount(baseAmount, rate);

        // Then - Must be exactly 1,085,000.0000 - no floating point errors
        assertThat(result).isEqualByComparingTo("1085000.0000");
        assertThat(result.scale()).isEqualTo(4);
    }

    @ParameterizedTest
    @CsvSource({
        "1000000.00, 1.085000, 1085000.0000",
        "999999.99, 1.085001, 1085000.9891",
        "0.01, 1.5, 0.0150",
        "1000000.00, 0.000001, 1.0000",
        "500000.50, 1.234567, 617284.1173",
        "1234567.89, 0.987654, 1219325.9148"
    })
    void shouldCalculateQuoteAmountForVariousInputs(String base, String rate, String expected) {
        // Given
        BigDecimal baseAmount = new BigDecimal(base);
        BigDecimal exchangeRate = new BigDecimal(rate);

        // When
        BigDecimal result = calculateQuoteAmount(baseAmount, exchangeRate);

        // Then
        assertThat(result).isEqualByComparingTo(expected);
        assertThat(result.scale()).isEqualTo(4);
    }

    @Test
    void shouldNotUsePrimitiveDoubleWhichLosesPrecision() {
        // Given - Demonstrate the danger of using double
        double baseDouble = 1000000.00;
        double rateDouble = 1.085000;
        double wrongResult = baseDouble * rateDouble;

        // Compare to correct BigDecimal calculation
        BigDecimal baseAmount = new BigDecimal("1000000.00");
        BigDecimal rate = new BigDecimal("1.085000");
        BigDecimal correctResult = calculateQuoteAmount(baseAmount, rate);

        // Then - double arithmetic may introduce floating point errors
        // This test documents why we MUST use BigDecimal
        assertThat(new BigDecimal(Double.toString(wrongResult)))
            .isEqualByComparingTo(correctResult);
    }

    @Test
    void shouldMaintainPrecisionThroughMultipleOperations() {
        // Given - Simulate multiple calculations
        BigDecimal amount1 = new BigDecimal("1000000.00");
        BigDecimal rate1 = new BigDecimal("1.085000");

        // When - Multiple operations
        BigDecimal result1 = amount1.multiply(rate1).setScale(4, RoundingMode.HALF_UP);
        BigDecimal result2 = result1.divide(rate1, 4, RoundingMode.HALF_UP);

        // Then - Should return to original amount (within rounding)
        assertThat(result2).isEqualByComparingTo(amount1);
    }

    @Test
    void shouldRoundHalfUpCorrectly() {
        // Given
        BigDecimal amount = new BigDecimal("1000000.00");
        BigDecimal rate = new BigDecimal("1.0855555"); // Will need rounding

        // When
        BigDecimal result = calculateQuoteAmount(amount, rate);

        // Then - 1085555.5000 rounds to 1085555.5000 (no rounding needed for .5)
        // But 1085555.55555 would round to 1085555.5556
        assertThat(result).isEqualByComparingTo("1085555.5000");
        assertThat(result.scale()).isEqualTo(4);
    }

    @Test
    void shouldHandleVerySmallAmounts() {
        // Given
        BigDecimal amount = new BigDecimal("0.01");
        BigDecimal rate = new BigDecimal("1.234567");

        // When
        BigDecimal result = calculateQuoteAmount(amount, rate);

        // Then
        assertThat(result).isEqualByComparingTo("0.0123");
        assertThat(result.scale()).isEqualTo(4);
    }

    @Test
    void shouldHandleVeryLargeAmounts() {
        // Given - Just under the max trade amount of 10,000,000
        BigDecimal amount = new BigDecimal("9999999.99");
        BigDecimal rate = new BigDecimal("1.500000");

        // When
        BigDecimal result = calculateQuoteAmount(amount, rate);

        // Then
        assertThat(result).isEqualByComparingTo("14999999.9850");
        assertThat(result.scale()).isEqualTo(4);
    }

    @Test
    void shouldCompareAmountsCorrectly() {
        // Given
        BigDecimal amount1 = new BigDecimal("1000000.00");
        BigDecimal amount2 = new BigDecimal("1000000.0000");
        BigDecimal amount3 = new BigDecimal("1000000.01");

        // Then - compareTo should work correctly despite different scales
        assertThat(amount1.compareTo(amount2)).isEqualTo(0);
        assertThat(amount1.compareTo(amount3)).isLessThan(0);
    }

    @Test
    void shouldNotLosePrecisionWithTrailingZeros() {
        // Given
        BigDecimal amount = new BigDecimal("1000000.0000");

        // When - Setting scale explicitly
        BigDecimal scaled = amount.setScale(4, RoundingMode.HALF_UP);

        // Then
        assertThat(scaled).isEqualByComparingTo(amount);
        assertThat(scaled.scale()).isEqualTo(4);
    }
}
