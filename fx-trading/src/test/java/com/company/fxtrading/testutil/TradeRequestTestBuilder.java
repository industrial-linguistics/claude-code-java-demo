package com.company.fxtrading.testutil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder for creating Trade request payloads for API tests.
 */
public class TradeRequestTestBuilder {
    private Map<String, Object> request = new HashMap<>();

    public static TradeRequestTestBuilder aTradeRequest() {
        return new TradeRequestTestBuilder().withDefaults();
    }

    public TradeRequestTestBuilder withDefaults() {
        request.put("tradeDate", LocalDate.now().toString());
        request.put("valueDate", LocalDate.now().plusDays(2).toString());
        request.put("direction", "BUY");
        request.put("baseCurrency", "EUR");
        request.put("quoteCurrency", "USD");
        request.put("baseAmount", "1000000.00");
        request.put("exchangeRate", "1.0850");
        request.put("trader", "Test Trader");
        return this;
    }

    public TradeRequestTestBuilder withTradeDate(String date) {
        request.put("tradeDate", date);
        return this;
    }

    public TradeRequestTestBuilder withValueDate(String date) {
        request.put("valueDate", date);
        return this;
    }

    public TradeRequestTestBuilder withDirection(String direction) {
        request.put("direction", direction);
        return this;
    }

    public TradeRequestTestBuilder withBaseCurrency(String currency) {
        request.put("baseCurrency", currency);
        return this;
    }

    public TradeRequestTestBuilder withQuoteCurrency(String currency) {
        request.put("quoteCurrency", currency);
        return this;
    }

    public TradeRequestTestBuilder withBaseAmount(String amount) {
        request.put("baseAmount", amount);
        return this;
    }

    public TradeRequestTestBuilder withExchangeRate(String rate) {
        request.put("exchangeRate", rate);
        return this;
    }

    public TradeRequestTestBuilder withCounterparty(String counterparty) {
        request.put("counterparty", counterparty);
        return this;
    }

    public TradeRequestTestBuilder withTrader(String trader) {
        request.put("trader", trader);
        return this;
    }

    public TradeRequestTestBuilder withNotes(String notes) {
        request.put("notes", notes);
        return this;
    }

    public Map<String, Object> build() {
        return request;
    }
}
