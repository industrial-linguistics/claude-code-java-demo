package com.company.fxtrading.repository;

import com.company.fxtrading.domain.TradeAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TradeAuditRepository extends JpaRepository<TradeAudit, Long> {

    List<TradeAudit> findByTradeIdOrderByAuditTimestampDescIdDesc(Long tradeId);

    List<TradeAudit> findByAuditTimestampBetweenOrderByAuditTimestampDescIdDesc(
            LocalDateTime start, LocalDateTime end);
}
