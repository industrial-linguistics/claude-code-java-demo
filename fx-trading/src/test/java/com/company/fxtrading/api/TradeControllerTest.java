package com.company.fxtrading.api;

import com.company.fxtrading.TradeTestBuilder;
import com.company.fxtrading.domain.Trade;
import com.company.fxtrading.domain.TradeStatus;
import com.company.fxtrading.service.TradeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API tests for TradeController.
 * Tests focus on HTTP endpoints, request/response format, and security.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("integration")
class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TradeService tradeService;

    @Test
    void shouldRequireAuthenticationForAllEndpoints() throws Exception {
        // When/Then - All endpoints require authentication
        mockMvc.perform(get("/api/trades"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/trades")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/trades/1"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/trades/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/trades/1/audit"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldCreateTradeAndReturnCreated() throws Exception {
        // Given
        String tradeJson = """
            {
                "tradeDate": "2025-10-12",
                "valueDate": "2025-10-14",
                "direction": "BUY",
                "baseCurrency": "EUR",
                "quoteCurrency": "USD",
                "baseAmount": 1000000.00,
                "exchangeRate": 1.0850,
                "counterparty": "Bank ABC",
                "trader": "John Doe",
                "notes": "Q1 hedging"
            }
            """;

        // When/Then
        mockMvc.perform(post("/api/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(tradeJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.tradeReference").exists())
            .andExpect(jsonPath("$.tradeReference").value(matchesPattern("FX-\\d{8}-\\d{4}")))
            .andExpect(jsonPath("$.baseCurrency").value("EUR"))
            .andExpect(jsonPath("$.quoteCurrency").value("USD"))
            .andExpect(jsonPath("$.baseAmount").value(1000000.00))
            .andExpect(jsonPath("$.exchangeRate").value(1.0850))
            .andExpect(jsonPath("$.quoteAmount").value(1085000.0000))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.createdBy").value("testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldGetAllTrades() throws Exception {
        // Given - Create test trades
        Trade trade1 = TradeTestBuilder.aTrade().build();
        Trade trade2 = TradeTestBuilder.aTrade().build();
        tradeService.recordTrade(trade1);
        tradeService.recordTrade(trade2);

        // When/Then
        mockMvc.perform(get("/api/trades"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$[0].tradeReference").exists())
            .andExpect(jsonPath("$[0].baseCurrency").exists());
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldGetTradeById() throws Exception {
        // Given
        Trade trade = TradeTestBuilder.aTrade()
            .withCurrencyPair("GBP", "USD")
            .build();
        Trade saved = tradeService.recordTrade(trade);

        // When/Then
        mockMvc.perform(get("/api/trades/" + saved.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(saved.getId()))
            .andExpect(jsonPath("$.tradeReference").value(saved.getTradeReference()))
            .andExpect(jsonPath("$.baseCurrency").value("GBP"))
            .andExpect(jsonPath("$.quoteCurrency").value("USD"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldReturn404WhenTradeNotFound() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/trades/99999"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldUpdateTrade() throws Exception {
        // Given
        Trade trade = TradeTestBuilder.aTrade()
            .withStatus(TradeStatus.PENDING)
            .build();
        Trade saved = tradeService.recordTrade(trade);

        String updateJson = """
            {
                "status": "CONFIRMED",
                "notes": "Trade confirmed with counterparty"
            }
            """;

        // When/Then
        mockMvc.perform(put("/api/trades/" + saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(saved.getId()))
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.notes").value("Trade confirmed with counterparty"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldGetAuditHistory() throws Exception {
        // Given - Create and update a trade
        Trade trade = TradeTestBuilder.aTrade()
            .withStatus(TradeStatus.PENDING)
            .build();
        Trade saved = tradeService.recordTrade(trade);

        Trade update = new Trade();
        update.setStatus(TradeStatus.CONFIRMED);
        tradeService.updateTrade(saved.getId(), update);

        // When/Then
        mockMvc.perform(get("/api/trades/" + saved.getId() + "/audit"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2)) // CREATE + UPDATE
            .andExpect(jsonPath("$[0].action").value("UPDATE")) // Most recent first
            .andExpect(jsonPath("$[0].auditUser").value("testuser"))
            .andExpect(jsonPath("$[0].beforeSnapshot").exists())
            .andExpect(jsonPath("$[0].afterSnapshot").exists())
            .andExpect(jsonPath("$[1].action").value("CREATE"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldFilterTradesByDateRange() throws Exception {
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

        // When/Then - Query October trades
        mockMvc.perform(get("/api/trades")
                .param("startDate", "2025-10-01")
                .param("endDate", "2025-10-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$[*].tradeDate", everyItem(
                anyOf(
                    equalTo("2025-10-01"),
                    equalTo("2025-10-15")
                )
            )));
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldFilterTradesByStatus() throws Exception {
        // Given
        Trade pending = TradeTestBuilder.aTrade()
            .withStatus(TradeStatus.PENDING)
            .build();
        Trade confirmed = TradeTestBuilder.aTrade()
            .withStatus(TradeStatus.CONFIRMED)
            .build();

        tradeService.recordTrade(pending);
        tradeService.recordTrade(confirmed);

        // When/Then - Query pending trades
        mockMvc.perform(get("/api/trades")
                .param("status", "PENDING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[*].status", everyItem(equalTo("PENDING"))));
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnValidationErrorForInvalidTradeData() throws Exception {
        // Given - Invalid JSON (missing required fields)
        String invalidJson = """
            {
                "direction": "BUY"
            }
            """;

        // When/Then
        mockMvc.perform(post("/api/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldHandleJsonDateFormatCorrectly() throws Exception {
        // Given
        String tradeJson = """
            {
                "tradeDate": "2025-10-12",
                "valueDate": "2025-10-14",
                "direction": "BUY",
                "baseCurrency": "EUR",
                "quoteCurrency": "USD",
                "baseAmount": 1000000.00,
                "exchangeRate": 1.0850
            }
            """;

        // When/Then - Dates should be parsed correctly
        mockMvc.perform(post("/api/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(tradeJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tradeDate").value("2025-10-12"))
            .andExpect(jsonPath("$.valueDate").value("2025-10-14"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnCorrectContentType() throws Exception {
        // Given
        Trade trade = TradeTestBuilder.aTrade().build();
        Trade saved = tradeService.recordTrade(trade);

        // When/Then
        mockMvc.perform(get("/api/trades/" + saved.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldHandleBigDecimalPrecisionInJsonResponse() throws Exception {
        // Given
        Trade trade = TradeTestBuilder.aTrade()
            .withAmount("1234567.8901")
            .withRate("1.234567")
            .build();
        Trade saved = tradeService.recordTrade(trade);

        // When/Then - Precision should be maintained in JSON
        mockMvc.perform(get("/api/trades/" + saved.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.baseAmount").value(1234567.8901))
            .andExpect(jsonPath("$.exchangeRate").value(1.234567));
    }
}
