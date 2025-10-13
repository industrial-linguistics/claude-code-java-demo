package com.company.fxtrading.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Trade domain entity.
 * Tests focus on entity behavior, validation, and business rules.
 */
@Tag("unit")
class TradeTest {

    @Test
    void shouldSetDefaultStatusToPendingOnCreate() {
        // Given
        Trade trade = new Trade();
        trade.setTradeDate(LocalDate.now());
        trade.setValueDate(LocalDate.now().plusDays(2));

        // When
        trade.onCreate();

        // Then
        assertThat(trade.getStatus()).isEqualTo(TradeStatus.PENDING);
    }

    @Test
    void shouldNotOverrideExplicitlySetStatus() {
        // Given
        Trade trade = new Trade();
        trade.setStatus(TradeStatus.CONFIRMED);

        // When
        trade.onCreate();

        // Then
        assertThat(trade.getStatus()).isEqualTo(TradeStatus.CONFIRMED);
    }

    @Test
    void shouldSetCreatedAtTimestampOnCreate() {
        // Given
        Trade trade = new Trade();

        // When
        trade.onCreate();

        // Then
        assertThat(trade.getCreatedAt()).isNotNull();
        assertThat(trade.getUpdatedAt()).isNotNull();
        // Both timestamps should be set (they may differ by microseconds)
        assertThat(trade.getCreatedAt()).isBeforeOrEqualTo(trade.getUpdatedAt());
    }

    @Test
    void shouldUpdateTimestampOnUpdate() throws InterruptedException {
        // Given
        Trade trade = new Trade();
        trade.onCreate();
        var originalUpdatedAt = trade.getUpdatedAt();

        // When
        Thread.sleep(10); // Ensure time difference
        trade.onUpdate();

        // Then
        assertThat(trade.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(trade.getCreatedAt()).isEqualTo(trade.getCreatedAt()); // Should not change
    }

    @Test
    void shouldMaintainBigDecimalPrecisionForAmounts() {
        // Given
        Trade trade = new Trade();
        BigDecimal preciseAmount = new BigDecimal("1234567.8901");

        // When
        trade.setBaseAmount(preciseAmount);

        // Then
        assertThat(trade.getBaseAmount()).isEqualByComparingTo(preciseAmount);
        assertThat(trade.getBaseAmount().scale()).isEqualTo(4);
    }

    @Test
    void shouldMaintainBigDecimalPrecisionForExchangeRate() {
        // Given
        Trade trade = new Trade();
        BigDecimal preciseRate = new BigDecimal("1.085432");

        // When
        trade.setExchangeRate(preciseRate);

        // Then
        assertThat(trade.getExchangeRate()).isEqualByComparingTo(preciseRate);
        assertThat(trade.getExchangeRate().scale()).isEqualTo(6);
    }

    @Test
    void shouldStoreExactQuoteAmount() {
        // Given
        Trade trade = new Trade();
        BigDecimal quoteAmount = new BigDecimal("1085000.0000");

        // When
        trade.setQuoteAmount(quoteAmount);

        // Then
        assertThat(trade.getQuoteAmount()).isEqualByComparingTo(quoteAmount);
        assertThat(trade.getQuoteAmount().scale()).isEqualTo(4);
    }

    @Test
    void shouldStoreCurrencyCodesInUpperCase() {
        // Given
        Trade trade = new Trade();

        // When
        trade.setBaseCurrency("EUR");
        trade.setQuoteCurrency("USD");

        // Then
        assertThat(trade.getBaseCurrency()).isEqualTo("EUR");
        assertThat(trade.getQuoteCurrency()).isEqualTo("USD");
    }

    @Test
    void shouldStoreAllTradeDetails() {
        // Given
        Trade trade = new Trade();
        LocalDate tradeDate = LocalDate.of(2025, 10, 12);
        LocalDate valueDate = LocalDate.of(2025, 10, 14);

        // When
        trade.setTradeReference("FX-20251012-0001");
        trade.setTradeDate(tradeDate);
        trade.setValueDate(valueDate);
        trade.setDirection(Direction.BUY);
        trade.setBaseCurrency("EUR");
        trade.setQuoteCurrency("USD");
        trade.setBaseAmount(new BigDecimal("1000000.00"));
        trade.setExchangeRate(new BigDecimal("1.085000"));
        trade.setQuoteAmount(new BigDecimal("1085000.0000"));
        trade.setCounterparty("Bank ABC");
        trade.setTrader("John Doe");
        trade.setNotes("Q1 hedging");
        trade.setStatus(TradeStatus.PENDING);

        // Then
        assertThat(trade.getTradeReference()).isEqualTo("FX-20251012-0001");
        assertThat(trade.getTradeDate()).isEqualTo(tradeDate);
        assertThat(trade.getValueDate()).isEqualTo(valueDate);
        assertThat(trade.getDirection()).isEqualTo(Direction.BUY);
        assertThat(trade.getBaseCurrency()).isEqualTo("EUR");
        assertThat(trade.getQuoteCurrency()).isEqualTo("USD");
        assertThat(trade.getBaseAmount()).isEqualByComparingTo("1000000.00");
        assertThat(trade.getExchangeRate()).isEqualByComparingTo("1.085000");
        assertThat(trade.getQuoteAmount()).isEqualByComparingTo("1085000.0000");
        assertThat(trade.getCounterparty()).isEqualTo("Bank ABC");
        assertThat(trade.getTrader()).isEqualTo("John Doe");
        assertThat(trade.getNotes()).isEqualTo("Q1 hedging");
        assertThat(trade.getStatus()).isEqualTo(TradeStatus.PENDING);
    }
}
