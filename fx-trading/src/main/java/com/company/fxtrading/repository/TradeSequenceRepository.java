package com.company.fxtrading.repository;

import com.company.fxtrading.domain.TradeSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Repository for trade sequence management.
 * Used to generate unique trade references in a thread-safe manner.
 */
@Repository
public interface TradeSequenceRepository extends JpaRepository<TradeSequence, LocalDate> {

    /**
     * Find sequence for a specific date with pessimistic write lock.
     * This ensures that only one thread can read and update the sequence at a time.
     *
     * @param tradeDate the date to find sequence for
     * @return the sequence entity with lock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ts FROM TradeSequence ts WHERE ts.tradeDate = :tradeDate")
    Optional<TradeSequence> findByTradeDateWithLock(LocalDate tradeDate);
}
