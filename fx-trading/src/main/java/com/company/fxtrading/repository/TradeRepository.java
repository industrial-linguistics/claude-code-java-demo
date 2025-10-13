package com.company.fxtrading.repository;

import com.company.fxtrading.domain.Trade;
import com.company.fxtrading.domain.TradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

    Optional<Trade> findByTradeReference(String tradeReference);

    List<Trade> findByTradeDateBetween(LocalDate startDate, LocalDate endDate);

    List<Trade> findByStatus(TradeStatus status);

    List<Trade> findByTrader(String trader);

    @Query("SELECT t FROM Trade t WHERE t.tradeDate >= :fromDate ORDER BY t.tradeDate DESC")
    List<Trade> findRecentTrades(@Param("fromDate") LocalDate fromDate);

    @Query("SELECT COUNT(t) FROM Trade t WHERE t.tradeDate = :date")
    long countTradesByDate(@Param("date") LocalDate date);

    List<Trade> findAllByOrderByTradeDateDesc();
}
