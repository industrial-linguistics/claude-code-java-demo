package com.company.fxtrading.testutil;

import com.company.fxtrading.domain.Direction;
import com.company.fxtrading.domain.Trade;
import com.company.fxtrading.domain.TradeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fluent builder for creating Trade test data.
 * Provides sensible defaults and readable test setup.
 */
public class TradeTestBuilder {
    private Trade trade = new Trade();

    public static TradeTestBuilder aTrade() {
        return new TradeTestBuilder().withDefaults();
    }

    public TradeTestBuilder withDefaults() {
        trade.setTradeDate(LocalDate.now());
        trade.setValueDate(LocalDate.now().plusDays(2));
        trade.setDirection(Direction.BUY);
        trade.setBaseCurrency("EUR");
        trade.setQuoteCurrency("USD");
        trade.setBaseAmount(new BigDecimal("1000000"));
        trade.setExchangeRate(new BigDecimal("1.0850"));
        trade.setQuoteAmount(new BigDecimal("1085000.0000"));
        trade.setStatus(TradeStatus.PENDING);
        trade.setTrader("Test Trader");
        trade.setCreatedBy("test-user");
        trade.setUpdatedBy("test-user");
        return this;
    }

    public TradeTestBuilder withTradeReference(String reference) {
        trade.setTradeReference(reference);
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

    public TradeTestBuilder withDirection(Direction direction) {
        trade.setDirection(direction);
        return this;
    }

    public TradeTestBuilder buy() {
        trade.setDirection(Direction.BUY);
        return this;
    }

    public TradeTestBuilder sell() {
        trade.setDirection(Direction.SELL);
        return this;
    }

    public TradeTestBuilder withBaseCurrency(String currency) {
        trade.setBaseCurrency(currency);
        return this;
    }

    public TradeTestBuilder withQuoteCurrency(String currency) {
        trade.setQuoteCurrency(currency);
        return this;
    }

    public TradeTestBuilder withEurUsd() {
        trade.setBaseCurrency("EUR");
        trade.setQuoteCurrency("USD");
        return this;
    }

    public TradeTestBuilder withGbpUsd() {
        trade.setBaseCurrency("GBP");
        trade.setQuoteCurrency("USD");
        return this;
    }

    public TradeTestBuilder withUsdJpy() {
        trade.setBaseCurrency("USD");
        trade.setQuoteCurrency("JPY");
        return this;
    }

    public TradeTestBuilder withBaseAmount(String amount) {
        trade.setBaseAmount(new BigDecimal(amount));
        return this;
    }

    public TradeTestBuilder withBaseAmount(BigDecimal amount) {
        trade.setBaseAmount(amount);
        return this;
    }

    public TradeTestBuilder withExchangeRate(String rate) {
        trade.setExchangeRate(new BigDecimal(rate));
        return this;
    }

    public TradeTestBuilder withExchangeRate(BigDecimal rate) {
        trade.setExchangeRate(rate);
        return this;
    }

    public TradeTestBuilder withQuoteAmount(String amount) {
        trade.setQuoteAmount(new BigDecimal(amount));
        return this;
    }

    public TradeTestBuilder withQuoteAmount(BigDecimal amount) {
        trade.setQuoteAmount(amount);
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

    public TradeTestBuilder withNotes(String notes) {
        trade.setNotes(notes);
        return this;
    }

    public TradeTestBuilder withStatus(TradeStatus status) {
        trade.setStatus(status);
        return this;
    }

    public TradeTestBuilder pending() {
        trade.setStatus(TradeStatus.PENDING);
        return this;
    }

    public TradeTestBuilder confirmed() {
        trade.setStatus(TradeStatus.CONFIRMED);
        return this;
    }

    public TradeTestBuilder settled() {
        trade.setStatus(TradeStatus.SETTLED);
        return this;
    }

    public TradeTestBuilder withCreatedBy(String user) {
        trade.setCreatedBy(user);
        return this;
    }

    public TradeTestBuilder withUpdatedBy(String user) {
        trade.setUpdatedBy(user);
        return this;
    }

    public Trade build() {
        return trade;
    }
}
