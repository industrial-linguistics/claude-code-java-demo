package com.company.fxtrading.api;

import com.company.fxtrading.domain.Trade;
import com.company.fxtrading.domain.TradeAudit;
import com.company.fxtrading.domain.TradeStatus;
import com.company.fxtrading.service.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @PostMapping
    public ResponseEntity<Trade> createTrade(@RequestBody Trade trade) {
        Trade created = tradeService.recordTrade(trade);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Trade> getTrade(@PathVariable Long id) {
        Trade trade = tradeService.findById(id);
        return ResponseEntity.ok(trade);
    }

    @GetMapping
    public ResponseEntity<List<Trade>> getAllTrades(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) TradeStatus status) {

        List<Trade> trades;
        if (startDate != null && endDate != null) {
            trades = tradeService.findTradesByDateRange(startDate, endDate);
        } else if (status != null) {
            trades = tradeService.findTradesByStatus(status);
        } else {
            trades = tradeService.findAllTrades();
        }

        return ResponseEntity.ok(trades);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Trade> updateTrade(@PathVariable Long id, @RequestBody Trade updates) {
        Trade updated = tradeService.updateTrade(id, updates);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/audit")
    public ResponseEntity<List<TradeAudit>> getTradeAudit(@PathVariable Long id) {
        List<TradeAudit> audits = tradeService.getAuditHistory(id);
        return ResponseEntity.ok(audits);
    }
}
