package com.company.fxtrading.repository;

import com.company.fxtrading.TradeTestBuilder;
import com.company.fxtrading.domain.Trade;
import com.company.fxtrading.domain.TradeStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Repository tests for TradeRepository.
 * Tests focus on database constraints, queries, and SQLite-specific behavior.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Tag("integration")
class TradeRepositoryTest {

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldSaveAndLoadTrade() {
        // Given
        Trade trade = TradeTestBuilder.aTrade()
            .withReference("FX-20251012-0001")
            .build();

        // When
        Trade saved = tradeRepository.save(trade);
        Trade loaded = tradeRepository.findById(saved.getId()).orElseThrow();

        // Then
        assertThat(loaded.getTradeReference()).isEqualTo("FX-20251012-0001");
        assertThat(loaded.getBaseCurrency()).isEqualTo("EUR");
        assertThat(loaded.getQuoteCurrency()).isEqualTo("USD");
    }

    @Test
    void shouldEnforceTradeReferenceUniqueness() {
        // Given
        Trade trade1 = TradeTestBuilder.aTrade()
            .withReference("FX-20251012-0001")
            .build();
        Trade trade2 = TradeTestBuilder.aTrade()
            .withReference("FX-20251012-0001") // Duplicate!
            .build();

        // When
        tradeRepository.save(trade1);

        // Then - Should throw constraint violation
        // Note: SQLite wraps constraint violations as JpaSystemException instead of DataIntegrityViolationException
        assertThatThrownBy(() -> {
            tradeRepository.save(trade2);
            tradeRepository.flush(); // Force constraint check
        }).isInstanceOf(JpaSystemException.class)
          .hasMessageContaining("UNIQUE constraint failed");
    }

    @Test
    void shouldFindTradesByDateRange() {
        // Given
        Trade oct1 = TradeTestBuilder.aTrade()
            .withReference("FX-20251001-0001")
            .withTradeDate(LocalDate.of(2025, 10, 1))
            .build();
        Trade oct15 = TradeTestBuilder.aTrade()
            .withReference("FX-20251015-0001")
            .withTradeDate(LocalDate.of(2025, 10, 15))
            .build();
        Trade nov1 = TradeTestBuilder.aTrade()
            .withReference("FX-20251101-0001")
            .withTradeDate(LocalDate.of(2025, 11, 1))
            .build();

        tradeRepository.saveAll(List.of(oct1, oct15, nov1));

        // When
        List<Trade> octoberTrades = tradeRepository.findByTradeDateBetween(
            LocalDate.of(2025, 10, 1),
            LocalDate.of(2025, 10, 31)
        );

        // Then
        assertThat(octoberTrades).hasSize(2);
        assertThat(octoberTrades).extracting(Trade::getTradeDate)
            .allMatch(date -> !date.isBefore(LocalDate.of(2025, 10, 1)) &&
                              !date.isAfter(LocalDate.of(2025, 10, 31)));
    }

    @Test
    void shouldFindTradesByStatus() {
        // Given
        Trade pending = TradeTestBuilder.aTrade()
            .withReference("FX-20251012-0001")
            .withStatus(TradeStatus.PENDING)
            .build();
        Trade confirmed = TradeTestBuilder.aTrade()
            .withReference("FX-20251012-0002")
            .withStatus(TradeStatus.CONFIRMED)
            .build();
        Trade settled = TradeTestBuilder.aTrade()
            .withReference("FX-20251012-0003")
            .withStatus(TradeStatus.SETTLED)
            .build();

        tradeRepository.saveAll(List.of(pending, confirmed, settled));

        // When
        List<Trade> pendingTrades = tradeRepository.findByStatus(TradeStatus.PENDING);

        // Then
        assertThat(pendingTrades).hasSize(1);
        assertThat(pendingTrades.get(0).getStatus()).isEqualTo(TradeStatus.PENDING);
    }

    @Test
    void shouldFindAllTradesOrderedByTradeDateDesc() {
        // Given
        Trade trade1 = TradeTestBuilder.aTrade()
            .withReference("FX-20251001-0001")
            .withTradeDate(LocalDate.of(2025, 10, 1))
            .build();
        Trade trade2 = TradeTestBuilder.aTrade()
            .withReference("FX-20251015-0001")
            .withTradeDate(LocalDate.of(2025, 10, 15))
            .build();
        Trade trade3 = TradeTestBuilder.aTrade()
            .withReference("FX-20251010-0001")
            .withTradeDate(LocalDate.of(2025, 10, 10))
            .build();

        tradeRepository.saveAll(List.of(trade1, trade2, trade3));

        // When
        List<Trade> trades = tradeRepository.findAllByOrderByTradeDateDesc();

        // Then
        assertThat(trades).hasSize(3);
        assertThat(trades.get(0).getTradeDate()).isEqualTo(LocalDate.of(2025, 10, 15)); // Most recent first
        assertThat(trades.get(1).getTradeDate()).isEqualTo(LocalDate.of(2025, 10, 10));
        assertThat(trades.get(2).getTradeDate()).isEqualTo(LocalDate.of(2025, 10, 1));
    }

    @Test
    void shouldCountTradesByDate() {
        // Given
        LocalDate today = LocalDate.now();
        Trade trade1 = TradeTestBuilder.aTrade()
            .withReference("FX-" + today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-0001")
            .withTradeDate(today)
            .build();
        Trade trade2 = TradeTestBuilder.aTrade()
            .withReference("FX-" + today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-0002")
            .withTradeDate(today)
            .build();
        Trade trade3 = TradeTestBuilder.aTrade()
            .withReference("FX-20251001-0001")
            .withTradeDate(LocalDate.of(2025, 10, 1))
            .build();

        tradeRepository.saveAll(List.of(trade1, trade2, trade3));

        // When
        long countToday = tradeRepository.countTradesByDate(today);
        long countOct1 = tradeRepository.countTradesByDate(LocalDate.of(2025, 10, 1));

        // Then
        assertThat(countToday).isEqualTo(2);
        assertThat(countOct1).isEqualTo(1);
    }

    @Test
    void shouldNotLoseBigDecimalPrecisionInDatabaseRoundTrip() {
        // Given
        BigDecimal preciseAmount = new BigDecimal("1234567.8901");
        BigDecimal preciseRate = new BigDecimal("1.234567");

        Trade trade = TradeTestBuilder.aTrade()
            .withReference("FX-20251012-0001")
            .withAmount(preciseAmount.toPlainString())
            .withRate(preciseRate.toPlainString())
            .build();

        // When
        Trade saved = tradeRepository.save(trade);
        tradeRepository.flush();
        Trade loaded = tradeRepository.findById(saved.getId()).orElseThrow();

        // Then - No precision loss
        assertThat(loaded.getBaseAmount()).isEqualByComparingTo(preciseAmount);
        assertThat(loaded.getExchangeRate()).isEqualByComparingTo(preciseRate);
    }

    @Test
    void shouldHandleOptimisticLockingWithVersion() {
        // Given
        Trade trade = TradeTestBuilder.aTrade()
            .withReference("FX-20251012-0001")
            .build();
        Trade saved = tradeRepository.save(trade);
        tradeRepository.flush();

        // When - Simulate concurrent update by loading same entity twice and detaching
        Trade entity1 = tradeRepository.findById(saved.getId()).orElseThrow();
        Trade entity2 = tradeRepository.findById(saved.getId()).orElseThrow();

        // Detach entity2 to simulate it being loaded in a different session
        entityManager.detach(entity2);

        entity1.setNotes("Updated by user 1");
        tradeRepository.save(entity1);
        tradeRepository.flush();

        // Clear the entity manager to ensure entity2 is truly stale
        entityManager.clear();

        entity2.setNotes("Updated by user 2");

        // Then - Second save should fail due to version mismatch
        // Note: Spring wraps Jakarta OptimisticLockException as ObjectOptimisticLockingFailureException
        assertThatThrownBy(() -> {
            tradeRepository.save(entity2);
            tradeRepository.flush();
        }).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void shouldStoreAndRetrieveAllTradeFields() {
        // Given
        Trade trade = TradeTestBuilder.aTrade()
            .withReference("FX-20251012-0001")
            .withTradeDate(LocalDate.of(2025, 10, 12))
            .withValueDate(LocalDate.of(2025, 10, 14))
            .buyDirection()
            .withCurrencyPair("GBP", "JPY")
            .withAmount("500000.50")
            .withRate("180.125000")
            .withQuoteAmount("90062562.5000")
            .withCounterparty("Bank XYZ")
            .withTrader("Jane Doe")
            .withNotes("Q4 hedging strategy")
            .withStatus(TradeStatus.CONFIRMED)
            .build();

        // When
        Trade saved = tradeRepository.save(trade);
        tradeRepository.flush();
        Trade loaded = tradeRepository.findById(saved.getId()).orElseThrow();

        // Then - All fields preserved
        assertThat(loaded.getTradeReference()).isEqualTo("FX-20251012-0001");
        assertThat(loaded.getTradeDate()).isEqualTo(LocalDate.of(2025, 10, 12));
        assertThat(loaded.getValueDate()).isEqualTo(LocalDate.of(2025, 10, 14));
        assertThat(loaded.getDirection()).isEqualTo(com.company.fxtrading.domain.Direction.BUY);
        assertThat(loaded.getBaseCurrency()).isEqualTo("GBP");
        assertThat(loaded.getQuoteCurrency()).isEqualTo("JPY");
        assertThat(loaded.getBaseAmount()).isEqualByComparingTo("500000.50");
        assertThat(loaded.getExchangeRate()).isEqualByComparingTo("180.125000");
        assertThat(loaded.getQuoteAmount()).isEqualByComparingTo("90062562.5000");
        assertThat(loaded.getCounterparty()).isEqualTo("Bank XYZ");
        assertThat(loaded.getTrader()).isEqualTo("Jane Doe");
        assertThat(loaded.getNotes()).isEqualTo("Q4 hedging strategy");
        assertThat(loaded.getStatus()).isEqualTo(TradeStatus.CONFIRMED);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
        assertThat(loaded.getVersion()).isNotNull();
    }

    @Test
    void shouldHandleNullOptionalFields() {
        // Given - Create trade with minimal required fields
        Trade trade = new Trade();
        trade.setTradeReference("FX-20251012-9999");
        trade.setTradeDate(LocalDate.now());
        trade.setValueDate(LocalDate.now().plusDays(2));
        trade.setDirection(com.company.fxtrading.domain.Direction.BUY);
        trade.setBaseCurrency("EUR");
        trade.setQuoteCurrency("USD");
        trade.setBaseAmount(new BigDecimal("1000000.00"));
        trade.setExchangeRate(new BigDecimal("1.0850"));
        trade.setQuoteAmount(new BigDecimal("1085000.0000"));
        trade.setCreatedBy("testuser");
        trade.setUpdatedBy("testuser");
        // counterparty, trader, notes are null

        // When
        Trade saved = tradeRepository.save(trade);
        Trade loaded = tradeRepository.findById(saved.getId()).orElseThrow();

        // Then - Nulls are preserved
        assertThat(loaded.getCounterparty()).isNull();
        assertThat(loaded.getTrader()).isNull();
        assertThat(loaded.getNotes()).isNull();
    }
}
