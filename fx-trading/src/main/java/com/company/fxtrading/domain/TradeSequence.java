package com.company.fxtrading.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Database-backed sequence for generating unique trade references.
 * Each date has its own sequence counter to ensure uniqueness within a day.
 *
 * This entity is critical for preventing race conditions in trade reference generation.
 * SQLite's write serialization ensures only one thread can update a sequence at a time.
 */
@Entity
@Table(name = "trade_sequence")
@Data
public class TradeSequence {

    @Id
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "next_sequence", nullable = false)
    private Integer nextSequence;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @Version
    private Long version;

    /**
     * Atomically increments and returns the next sequence number.
     * This method should be called within a transaction.
     *
     * @return the next sequence number (before increment)
     */
    public Integer getAndIncrement() {
        Integer current = this.nextSequence;
        this.nextSequence = current + 1;
        this.lastUpdated = LocalDateTime.now();
        return current;
    }
}
