package com.company.fxtrading;

import com.company.fxtrading.domain.Direction;
import com.company.fxtrading.domain.Trade;
import com.company.fxtrading.domain.TradeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fluent builder for creating Trade test data.
 * Provides sensible defaults and readable test setup.
 */
public class TradeTestBuilder {
    private static final AtomicInteger counter = new AtomicInteger(1);
    private Trade trade = new Trade();

    public static TradeTestBuilder aTrade() {
        return new TradeTestBuilder().withDefaults();
    }

    public TradeTestBuilder withDefaults() {
        // Generate unique trade reference to avoid conflicts across tests
        String uniqueRef = "FX-TEST-" + String.format("%06d", counter.getAndIncrement());
        trade.setTradeReference(uniqueRef);
        trade.setTradeDate(LocalDate.now());
        trade.setValueDate(LocalDate.now().plusDays(2));
        trade.setDirection(Direction.BUY);
        trade.setBaseCurrency("EUR");
        trade.setQuoteCurrency("USD");
        trade.setBaseAmount(new BigDecimal("1000000.00"));
        trade.setExchangeRate(new BigDecimal("1.085000"));
        trade.setQuoteAmount(new BigDecimal("1085000.0000"));
        trade.setCounterparty("Bank ABC");
        trade.setTrader("Test Trader");
        trade.setStatus(TradeStatus.PENDING);
        trade.setCreatedBy("testuser");
        trade.setUpdatedBy("testuser");
        return this;
    }

    public TradeTestBuilder withTradeDate(LocalDate date) {
        trade.setTradeDate(date);
        return this;
    }

    public TradeTestBuilder withValueDate(LocalDate date) {
        trade.setValueDate(date);
        return this;
    }

    public TradeTestBuilder withEurUsd() {
        trade.setBaseCurrency("EUR");
        trade.setQuoteCurrency("USD");
        return this;
    }

    public TradeTestBuilder withCurrencyPair(String base, String quote) {
        trade.setBaseCurrency(base);
        trade.setQuoteCurrency(quote);
        return this;
    }

    public TradeTestBuilder buyDirection() {
        trade.setDirection(Direction.BUY);
        return this;
    }

    public TradeTestBuilder sellDirection() {
        trade.setDirection(Direction.SELL);
        return this;
    }

    public TradeTestBuilder withAmount(String amount) {
        trade.setBaseAmount(new BigDecimal(amount));
        return this;
    }

    public TradeTestBuilder withRate(String rate) {
        trade.setExchangeRate(new BigDecimal(rate));
        return this;
    }

    public TradeTestBuilder withQuoteAmount(String amount) {
        trade.setQuoteAmount(new BigDecimal(amount));
        return this;
    }

    public TradeTestBuilder withCounterparty(String counterparty) {
        trade.setCounterparty(counterparty);
        return this;
    }

    public TradeTestBuilder withTrader(String trader) {
        trade.setTrader(trader);
        return this;
    }

    public TradeTestBuilder withStatus(TradeStatus status) {
        trade.setStatus(status);
        return this;
    }

    public TradeTestBuilder withNotes(String notes) {
        trade.setNotes(notes);
        return this;
    }

    public TradeTestBuilder withReference(String reference) {
        trade.setTradeReference(reference);
        return this;
    }

    public Trade build() {
        return trade;
    }
}
