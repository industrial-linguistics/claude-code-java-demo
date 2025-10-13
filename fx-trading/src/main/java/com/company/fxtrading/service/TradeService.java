package com.company.fxtrading.service;

import com.company.fxtrading.domain.*;
import com.company.fxtrading.repository.TradeAuditRepository;
import com.company.fxtrading.repository.TradeRepository;
import com.company.fxtrading.repository.TradeSequenceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TradeService {

    private final TradeRepository tradeRepository;
    private final TradeAuditRepository auditRepository;
    private final TradeSequenceRepository sequenceRepository;
    private final ObjectMapper objectMapper;

    public Trade recordTrade(Trade trade) {
        // Validate required fields
        if (trade.getBaseAmount() == null) {
            throw new IllegalArgumentException("Base amount is required");
        }
        if (trade.getExchangeRate() == null) {
            throw new IllegalArgumentException("Exchange rate is required");
        }

        // Generate trade reference (synchronized to prevent race condition)
        trade.setTradeReference(generateTradeReference());

        // Calculate quote amount if not set
        if (trade.getQuoteAmount() == null) {
            trade.setQuoteAmount(calculateQuoteAmount(
                    trade.getBaseAmount(),
                    trade.getExchangeRate(),
                    trade.getDirection()
            ));
        }

        // Set audit fields
        String currentUser = getCurrentUser();
        trade.setCreatedBy(currentUser);
        trade.setUpdatedBy(currentUser);

        // Save trade
        Trade savedTrade = tradeRepository.save(trade);

        // Create audit record
        auditTrade(savedTrade, AuditAction.CREATE, null, savedTrade);

        return savedTrade;
    }

    public Trade updateTrade(Long id, Trade updates) {
        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trade not found: " + id));

        // Store before state for audit
        Trade beforeState = cloneTrade(trade);

        // Update fields
        if (updates.getStatus() != null) {
            trade.setStatus(updates.getStatus());
        }
        if (updates.getNotes() != null) {
            trade.setNotes(updates.getNotes());
        }
        if (updates.getCounterparty() != null) {
            trade.setCounterparty(updates.getCounterparty());
        }

        trade.setUpdatedBy(getCurrentUser());

        Trade updated = tradeRepository.save(trade);

        // Audit
        auditTrade(updated, AuditAction.UPDATE, beforeState, updated);

        return updated;
    }

    @Transactional(readOnly = true)
    public Trade findById(Long id) {
        return tradeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trade not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Trade> findAllTrades() {
        return tradeRepository.findAllByOrderByTradeDateDesc();
    }

    @Transactional(readOnly = true)
    public List<Trade> findTradesByDateRange(LocalDate startDate, LocalDate endDate) {
        return tradeRepository.findByTradeDateBetween(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<Trade> findTradesByStatus(TradeStatus status) {
        return tradeRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<TradeAudit> getAuditHistory(Long tradeId) {
        return auditRepository.findByTradeIdOrderByAuditTimestampDescIdDesc(tradeId);
    }

    private void auditTrade(Trade trade, AuditAction action, Trade before, Trade after) {
        TradeAudit audit = new TradeAudit();
        audit.setTradeId(trade.getId());
        audit.setTradeReference(trade.getTradeReference());
        audit.setAuditTimestamp(LocalDateTime.now());
        audit.setAuditUser(getCurrentUser());
        audit.setAction(action);
        audit.setBeforeSnapshot(toJson(before));
        audit.setAfterSnapshot(toJson(after));
        auditRepository.save(audit);
    }

    /**
     * Generate a unique trade reference using database-backed sequence.
     * Format: FX-YYYYMMDD-####
     *
     * This method uses pessimistic locking at the database level to prevent race conditions.
     * SQLite's write serialization ensures only one thread can update the sequence at a time.
     *
     * @return unique trade reference
     */
    private String generateTradeReference() {
        LocalDate today = LocalDate.now();
        String datePrefix = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Use pessimistic lock to ensure atomic read-and-increment
        TradeSequence sequence = sequenceRepository.findByTradeDateWithLock(today)
                .orElseGet(() -> {
                    // First trade of the day - create new sequence starting at 1
                    TradeSequence newSequence = new TradeSequence();
                    newSequence.setTradeDate(today);
                    newSequence.setNextSequence(1);
                    newSequence.setLastUpdated(LocalDateTime.now());
                    return sequenceRepository.save(newSequence);
                });

        // Get current sequence number and increment for next time
        Integer sequenceNumber = sequence.getAndIncrement();
        sequenceRepository.save(sequence);

        return String.format("FX-%s-%04d", datePrefix, sequenceNumber);
    }

    private BigDecimal calculateQuoteAmount(BigDecimal baseAmount, BigDecimal rate, Direction direction) {
        return baseAmount.multiply(rate).setScale(4, RoundingMode.HALF_UP);
    }

    private String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "system";
    }

    private Trade cloneTrade(Trade trade) {
        Trade clone = new Trade();
        clone.setId(trade.getId());
        clone.setTradeReference(trade.getTradeReference());
        clone.setTradeDate(trade.getTradeDate());
        clone.setValueDate(trade.getValueDate());
        clone.setDirection(trade.getDirection());
        clone.setBaseCurrency(trade.getBaseCurrency());
        clone.setQuoteCurrency(trade.getQuoteCurrency());
        clone.setBaseAmount(trade.getBaseAmount());
        clone.setExchangeRate(trade.getExchangeRate());
        clone.setQuoteAmount(trade.getQuoteAmount());
        clone.setCounterparty(trade.getCounterparty());
        clone.setTrader(trade.getTrader());
        clone.setNotes(trade.getNotes());
        clone.setStatus(trade.getStatus());
        return clone;
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }
}
