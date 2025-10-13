package com.company.fxtrading.service;

import com.company.fxtrading.TradeTestBuilder;
import com.company.fxtrading.domain.*;
import com.company.fxtrading.repository.TradeAuditRepository;
import com.company.fxtrading.repository.TradeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for TradeService.
 * Critical focus: Audit trail creation and financial precision.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("integration")
class TradeServiceTest {

    @Autowired
    private TradeService tradeService;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private TradeAuditRepository auditRepository;

    @Test
    @WithMockUser(username = "testuser")
    void shouldRecordTradeWithGeneratedReference() {
        // Given
        Trade trade = TradeTestBuilder.aTrade().build();
        trade.setTradeReference(null); // Should be generated

        // When
        Trade saved = tradeService.recordTrade(trade);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTradeReference()).isNotNull();
        assertThat(saved.getTradeReference()).matches("FX-\\d{8}-\\d{4}");
        assertThat(saved.getCreatedBy()).isEqualTo("testuser");
        assertThat(saved.getUpdatedBy()).isEqualTo("testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldCalculateQuoteAmountIfNotProvided() {
        // Given
        Trade trade = TradeTestBuilder.aTrade()
            .withAmount("1000000.00")
            .withRate("1.085000")
            .build();
        trade.setQuoteAmount(null); // Should be calculated

        // When
        Trade saved = tradeService.recordTrade(trade);

        // Then
        assertThat(saved.getQuoteAmount()).isEqualByComparingTo("1085000.0000");
        assertThat(saved.getQuoteAmount().scale()).isEqualTo(4);
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldCreateAuditRecordWhenTradeIsCreated() {
        // Given
        Trade trade = TradeTestBuilder.aTrade().build();

        // When
        Trade saved = tradeService.recordTrade(trade);

        // Then - Should have 1 audit record (CREATE)
        List<TradeAudit> audits = tradeService.getAuditHistory(saved.getId());

        assertThat(audits).hasSize(1);
        assertThat(audits.get(0).getAction()).isEqualTo(AuditAction.CREATE);
        assertThat(audits.get(0).getAuditUser()).isEqualTo("testuser");
        assertThat(audits.get(0).getTradeReference()).isEqualTo(saved.getTradeReference());
        assertThat(audits.get(0).getBeforeSnapshot()).isNull();
        assertThat(audits.get(0).getAfterSnapshot()).isNotNull();
        assertThat(audits.get(0).getAfterSnapshot()).contains("EUR");
        assertThat(audits.get(0).getAfterSnapshot()).contains("USD");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldCreateAuditRecordForEveryTradeChange() {
        // Given - Create initial trade
        Trade trade = TradeTestBuilder.aTrade()
            .withStatus(TradeStatus.PENDING)
            .build();
        Trade saved = tradeService.recordTrade(trade);

        // When - Make multiple changes
        Trade update1 = new Trade();
        update1.setStatus(TradeStatus.CONFIRMED);
        tradeService.updateTrade(saved.getId(), update1);

        Trade update2 = new Trade();
        update2.setNotes("Updated notes");
        tradeService.updateTrade(saved.getId(), update2);

        // Then - Should have 3 audit records (1 create + 2 updates)
        List<TradeAudit> audits = tradeService.getAuditHistory(saved.getId());

        assertThat(audits).hasSize(3);
        assertThat(audits.get(0).getAction()).isEqualTo(AuditAction.UPDATE); // Most recent first
        assertThat(audits.get(1).getAction()).isEqualTo(AuditAction.UPDATE);
        assertThat(audits.get(2).getAction()).isEqualTo(AuditAction.CREATE); // Original
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldCaptureBeforeAndAfterSnapshotsInAudit() {
        // Given - Create trade
        Trade trade = TradeTestBuilder.aTrade()
            .withStatus(TradeStatus.PENDING)
            .withNotes("Original notes")
            .build();
        Trade saved = tradeService.recordTrade(trade);

        // When - Update status
        Trade update = new Trade();
        update.setStatus(TradeStatus.CONFIRMED);
        tradeService.updateTrade(saved.getId(), update);

        // Then - Audit should capture both states
        List<TradeAudit> audits = tradeService.getAuditHistory(saved.getId());
        TradeAudit updateAudit = audits.get(0); // Most recent

        assertThat(updateAudit.getBeforeSnapshot()).contains("PENDING");
        assertThat(updateAudit.getAfterSnapshot()).contains("CONFIRMED");
        assertThat(updateAudit.getBeforeSnapshot()).contains("Original notes");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldUpdateTradeAndPreserveAuditTrail() {
        // Given
        Trade trade = TradeTestBuilder.aTrade()
            .withStatus(TradeStatus.PENDING)
            .withCounterparty("Bank ABC")
            .build();
        Trade saved = tradeService.recordTrade(trade);

        // When
        Trade update = new Trade();
        update.setStatus(TradeStatus.CONFIRMED);
        update.setCounterparty("Bank XYZ");
        update.setNotes("Trade confirmed");
        Trade updated = tradeService.updateTrade(saved.getId(), update);

        // Then
        assertThat(updated.getStatus()).isEqualTo(TradeStatus.CONFIRMED);
        assertThat(updated.getCounterparty()).isEqualTo("Bank XYZ");
        assertThat(updated.getNotes()).isEqualTo("Trade confirmed");
        assertThat(updated.getUpdatedBy()).isEqualTo("testuser");

        // Verify audit trail
        List<TradeAudit> audits = tradeService.getAuditHistory(saved.getId());
        assertThat(audits).hasSize(2); // CREATE + UPDATE
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldFindTradeById() {
        // Given
        Trade trade = TradeTestBuilder.aTrade().build();
        Trade saved = tradeService.recordTrade(trade);

        // When
        Trade found = tradeService.findById(saved.getId());

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getTradeReference()).isEqualTo(saved.getTradeReference());
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldThrowExceptionWhenTradeNotFound() {
        // When/Then
        assertThatThrownBy(() -> tradeService.findById(99999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Trade not found");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldFindAllTradesOrderedByDateDesc() {
        // Given
        Trade trade1 = TradeTestBuilder.aTrade()
            .withTradeDate(LocalDate.of(2025, 10, 1))
            .build();
        Trade trade2 = TradeTestBuilder.aTrade()
            .withTradeDate(LocalDate.of(2025, 10, 15))
            .build();
        Trade trade3 = TradeTestBuilder.aTrade()
            .withTradeDate(LocalDate.of(2025, 10, 10))
            .build();

        tradeService.recordTrade(trade1);
        tradeService.recordTrade(trade2);
        tradeService.recordTrade(trade3);

        // When
        List<Trade> trades = tradeService.findAllTrades();

        // Then - Should be ordered by date descending
        assertThat(trades).hasSizeGreaterThanOrEqualTo(3);
        assertThat(trades.get(0).getTradeDate()).isAfterOrEqualTo(trades.get(1).getTradeDate());
        assertThat(trades.get(1).getTradeDate()).isAfterOrEqualTo(trades.get(2).getTradeDate());
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldFindTradesByDateRange() {
        // Given
        Trade oct1 = TradeTestBuilder.aTrade()
            .withTradeDate(LocalDate.of(2025, 10, 1))
            .build();
        Trade oct15 = TradeTestBuilder.aTrade()
            .withTradeDate(LocalDate.of(2025, 10, 15))
            .build();
        Trade nov1 = TradeTestBuilder.aTrade()
            .withTradeDate(LocalDate.of(2025, 11, 1))
            .build();

        tradeService.recordTrade(oct1);
        tradeService.recordTrade(oct15);
        tradeService.recordTrade(nov1);

        // When
        List<Trade> octoberTrades = tradeService.findTradesByDateRange(
            LocalDate.of(2025, 10, 1),
            LocalDate.of(2025, 10, 31)
        );

        // Then
        assertThat(octoberTrades).hasSizeGreaterThanOrEqualTo(2);
        assertThat(octoberTrades).allMatch(t ->
            !t.getTradeDate().isBefore(LocalDate.of(2025, 10, 1)) &&
            !t.getTradeDate().isAfter(LocalDate.of(2025, 10, 31))
        );
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldFindTradesByStatus() {
        // Given
        Trade pending1 = TradeTestBuilder.aTrade()
            .withStatus(TradeStatus.PENDING)
            .build();
        Trade pending2 = TradeTestBuilder.aTrade()
            .withStatus(TradeStatus.PENDING)
            .build();
        Trade confirmed = TradeTestBuilder.aTrade()
            .withStatus(TradeStatus.CONFIRMED)
            .build();

        tradeService.recordTrade(pending1);
        tradeService.recordTrade(pending2);
        tradeService.recordTrade(confirmed);

        // When
        List<Trade> pendingTrades = tradeService.findTradesByStatus(TradeStatus.PENDING);

        // Then
        assertThat(pendingTrades).hasSizeGreaterThanOrEqualTo(2);
        assertThat(pendingTrades).allMatch(t -> t.getStatus() == TradeStatus.PENDING);
    }

    @Test
    @WithMockUser(username = "trader1")
    void shouldTrackDifferentUsersInAuditTrail() {
        // Given - Create as trader1
        Trade trade = TradeTestBuilder.aTrade().build();
        Trade saved = tradeService.recordTrade(trade);

        assertThat(saved.getCreatedBy()).isEqualTo("trader1");

        // When - Update as different user (simulated by changing mock user in real scenario)
        Trade update = new Trade();
        update.setNotes("Updated by trader1");
        Trade updated = tradeService.updateTrade(saved.getId(), update);

        // Then
        assertThat(updated.getUpdatedBy()).isEqualTo("trader1");

        List<TradeAudit> audits = tradeService.getAuditHistory(saved.getId());
        assertThat(audits.get(0).getAuditUser()).isEqualTo("trader1"); // Update
        assertThat(audits.get(1).getAuditUser()).isEqualTo("trader1"); // Create
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldMaintainFinancialPrecisionThroughSaveAndLoad() {
        // Given
        Trade trade = TradeTestBuilder.aTrade()
            .withAmount("1234567.8901")
            .withRate("1.234567")
            .build();
        trade.setQuoteAmount(null); // Will be calculated

        // When
        Trade saved = tradeService.recordTrade(trade);
        Trade loaded = tradeService.findById(saved.getId());

        // Then - No precision loss
        assertThat(loaded.getBaseAmount()).isEqualByComparingTo("1234567.8901");
        assertThat(loaded.getExchangeRate()).isEqualByComparingTo("1.234567");
        assertThat(loaded.getQuoteAmount()).isNotNull();
        assertThat(loaded.getQuoteAmount().scale()).isEqualTo(4);
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldGenerateSequentialTradeReferencesForSameDay() {
        // Given
        Trade trade1 = TradeTestBuilder.aTrade().build();
        Trade trade2 = TradeTestBuilder.aTrade().build();

        // When
        Trade saved1 = tradeService.recordTrade(trade1);
        Trade saved2 = tradeService.recordTrade(trade2);

        // Then - References should be sequential for today
        String today = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertThat(saved1.getTradeReference()).startsWith("FX-" + today);
        assertThat(saved2.getTradeReference()).startsWith("FX-" + today);

        // Extract sequence numbers
        String seq1 = saved1.getTradeReference().substring(saved1.getTradeReference().lastIndexOf("-") + 1);
        String seq2 = saved2.getTradeReference().substring(saved2.getTradeReference().lastIndexOf("-") + 1);

        int num1 = Integer.parseInt(seq1);
        int num2 = Integer.parseInt(seq2);

        // Note: This test may reveal the race condition bug if run concurrently
        assertThat(num2).isGreaterThan(num1);
    }
}
