# Testing Strategy for FX Trading System

## Overview

This is a financial transaction system. **Every feature must have tests before implementation.**

The FX Trading System records financial transactions with full audit trails. Given the low transaction volume (~50 trades/week) but high accuracy requirements, bugs could lurk undetected for weeks. A comprehensive test suite is essential.

## Current State

**WARNING**: The initial implementation was created without tests. This document describes what should have been done and serves as a guide for future development.

## Test-Driven Development Approach

### Why TDD is Critical Here

1. **Financial System** - Accuracy is non-negotiable
2. **Low Volume** - Bugs won't surface quickly through usage
3. **Weak Ops Team** - Debugging in production is high-risk
4. **Audit Requirements** - Every change must be tracked correctly
5. **SQLite Concurrency** - Write serialization needs verification

### Red-Green-Refactor Workflow

When implementing any feature:

1. **RED**: Write a failing test first
   ```java
   @Test
   void shouldCalculateQuoteAmountCorrectly() {
       // Test fails because method doesn't exist yet
   }
   ```

2. **GREEN**: Implement minimal code to pass
   ```java
   public BigDecimal calculateQuoteAmount(...) {
       return baseAmount.multiply(rate);
   }
   ```

3. **REFACTOR**: Improve while keeping tests green
   ```java
   public BigDecimal calculateQuoteAmount(...) {
       return baseAmount.multiply(rate)
           .setScale(4, RoundingMode.HALF_UP);
   }
   ```

4. **VERIFY AUDIT**: For state-changing operations, always verify audit trail creation

## Testing Priorities

### 1. Domain Logic Tests (Critical)

**Location**: `src/test/java/com/company/fxtrading/domain/`

**What to test**:
- Trade validation (currency codes, amounts, dates)
- Quote amount calculation with BigDecimal precision
- Trade reference generation and uniqueness
- Status transition rules
- Value objects and enums

**Example critical test**:
```java
@Test
void shouldCalculateQuoteAmountWithCorrectPrecision() {
    BigDecimal baseAmount = new BigDecimal("1000000.00");
    BigDecimal rate = new BigDecimal("1.085000");

    BigDecimal result = calculateQuoteAmount(baseAmount, rate, Direction.BUY);

    // Must be exactly 1,085,000.0000 - no floating point errors
    assertThat(result).isEqualByComparingTo("1085000.0000");
    assertThat(result.scale()).isEqualTo(4);
}
```

**Critical edge cases**:
- Negative amounts (should reject)
- Invalid currency codes (should reject)
- Value date before trade date (should reject)
- Amounts exceeding max configured limit (should reject)
- BigDecimal precision in multiplication/division
- Status transitions (can't go SETTLED → PENDING)

### 2. Repository Tests (High Priority)

**Location**: `src/test/java/com/company/fxtrading/repository/`

**What to test**:
- SQLite-specific behavior (WAL mode, concurrency)
- Unique constraint violations
- Date range queries
- Optimistic locking (version field)
- Index usage for performance

**Example critical test**:
```java
@DataJpaTest
class TradeRepositoryTest {

    @Test
    void shouldEnforceTradeReferenceUniqueness() {
        Trade trade1 = createTrade("FX-20251012-0001");
        Trade trade2 = createTrade("FX-20251012-0001");

        repository.save(trade1);

        assertThatThrownBy(() -> repository.save(trade2))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldFindTradesByDateRange() {
        // Create trades across multiple dates
        Trade oct1 = createTradeOnDate(LocalDate.of(2025, 10, 1));
        Trade oct15 = createTradeOnDate(LocalDate.of(2025, 10, 15));
        Trade nov1 = createTradeOnDate(LocalDate.of(2025, 11, 1));

        repository.saveAll(List.of(oct1, oct15, nov1));

        List<Trade> octoberTrades = repository.findByTradeDateBetween(
            LocalDate.of(2025, 10, 1),
            LocalDate.of(2025, 10, 31)
        );

        assertThat(octoberTrades).hasSize(2);
        assertThat(octoberTrades).doesNotContain(nov1);
    }
}
```

### 3. Service Layer Tests (High Priority)

**Location**: `src/test/java/com/company/fxtrading/service/`

**What to test**:
- Transaction boundaries
- Audit record creation on every change
- User context propagation
- Error handling and rollback
- Business logic orchestration

**Example critical test**:
```java
@SpringBootTest
@Transactional
class TradeServiceTest {

    @Test
    void shouldCreateAuditRecordForEveryTradeChange() {
        // Given
        Trade trade = tradeService.recordTrade(createTradeRequest());

        // When - make multiple changes
        trade.setStatus(TradeStatus.CONFIRMED);
        tradeService.updateTrade(trade.getId(), trade);

        trade.setNotes("Updated notes");
        tradeService.updateTrade(trade.getId(), trade);

        // Then - should have 3 audit records (1 create + 2 updates)
        List<TradeAudit> audits = tradeService.getAuditHistory(trade.getId());

        assertThat(audits).hasSize(3);
        assertThat(audits.get(0).getAction()).isEqualTo(AuditAction.UPDATE);
        assertThat(audits.get(1).getAction()).isEqualTo(AuditAction.UPDATE);
        assertThat(audits.get(2).getAction()).isEqualTo(AuditAction.CREATE);
    }

    @Test
    void shouldCaptureBeforeAndAfterSnapshotsInAudit() {
        // Given
        Trade trade = tradeService.recordTrade(createTradeRequest());

        // When
        Trade updates = new Trade();
        updates.setStatus(TradeStatus.CONFIRMED);
        tradeService.updateTrade(trade.getId(), updates);

        // Then
        List<TradeAudit> audits = tradeService.getAuditHistory(trade.getId());
        TradeAudit updateAudit = audits.get(0); // Most recent

        assertThat(updateAudit.getBeforeSnapshot()).contains("PENDING");
        assertThat(updateAudit.getAfterSnapshot()).contains("CONFIRMED");
    }

    @Test
    void shouldRollbackOnError() {
        // Test that if audit creation fails, trade creation also rolls back
    }
}
```

### 4. API Tests (Medium Priority)

**Location**: `src/test/java/com/company/fxtrading/api/`

**What to test**:
- Request validation
- Response formats (JSON structure)
- HTTP status codes
- Security (authentication required)
- Error responses

**Example test**:
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "testuser")
    void shouldCreateTradeAndReturnCreated() throws Exception {
        String tradeJson = """
            {
                "tradeDate": "2025-10-12",
                "valueDate": "2025-10-14",
                "direction": "BUY",
                "baseCurrency": "EUR",
                "quoteCurrency": "USD",
                "baseAmount": 1000000.00,
                "exchangeRate": 1.0850,
                "trader": "John Doe"
            }
            """;

        mockMvc.perform(post("/api/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(tradeJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tradeReference").exists())
            .andExpect(jsonPath("$.quoteAmount").value(1085000.0000));
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/trades"))
            .andExpect(status().isUnauthorized());
    }
}
```

### 5. Integration Tests (Medium Priority)

**Location**: `src/test/java/com/company/fxtrading/integration/`

**What to test**:
- Full end-to-end trade recording flow
- Database schema via Liquibase
- Audit trail integrity across layers
- Multi-step workflows

**Example test**:
```java
@SpringBootTest
@Transactional
class TradeLifecycleIntegrationTest {

    @Test
    void shouldCompleteFullTradeLifecycle() {
        // Create trade
        Trade trade = tradeService.recordTrade(createTradeRequest());
        assertThat(trade.getStatus()).isEqualTo(TradeStatus.PENDING);

        // Confirm trade
        trade.setStatus(TradeStatus.CONFIRMED);
        trade = tradeService.updateTrade(trade.getId(), trade);
        assertThat(trade.getStatus()).isEqualTo(TradeStatus.CONFIRMED);

        // Settle trade
        trade.setStatus(TradeStatus.SETTLED);
        trade = tradeService.updateTrade(trade.getId(), trade);
        assertThat(trade.getStatus()).isEqualTo(TradeStatus.SETTLED);

        // Verify audit trail
        List<TradeAudit> audits = tradeService.getAuditHistory(trade.getId());
        assertThat(audits).hasSize(3); // CREATE + 2 status changes
    }
}
```

## Critical Test Cases

### Concurrency Tests

**CRITICAL ISSUE**: The current `generateTradeReference()` implementation has a race condition:

```java
// BROKEN: Two threads can get same count
long countToday = tradeRepository.countTradesByDate(LocalDate.now());
return String.format("FX-%s-%04d", datePrefix, countToday + 1);
```

**Required test**:
```java
@Test
void shouldGenerateUniqueTradeReferencesUnderConcurrentLoad() throws Exception {
    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    Set<String> references = ConcurrentHashMap.newKeySet();

    List<Future<?>> futures = new ArrayList<>();
    for (int i = 0; i < threadCount; i++) {
        futures.add(executor.submit(() -> {
            Trade trade = tradeService.recordTrade(createTradeRequest());
            references.add(trade.getTradeReference());
        }));
    }

    // Wait for all threads
    for (Future<?> future : futures) {
        future.get();
    }

    // All references must be unique
    assertThat(references).hasSize(threadCount);
}
```

**Fix options**:
1. Use database sequence for reference number
2. Add pessimistic lock around reference generation
3. Use UUID-based references

### SQLite Concurrency Tests

```java
@Test
void shouldHandleMultipleConcurrentReads() throws Exception {
    // 12 users reading simultaneously (your actual use case)
    int readerCount = 12;
    ExecutorService executor = Executors.newFixedThreadPool(readerCount);

    List<Future<List<Trade>>> futures = new ArrayList<>();
    for (int i = 0; i < readerCount; i++) {
        futures.add(executor.submit(() ->
            tradeService.findAllTrades()
        ));
    }

    // All reads should succeed without timeout
    for (Future<List<Trade>> future : futures) {
        List<Trade> trades = future.get(5, TimeUnit.SECONDS);
        assertThat(trades).isNotNull();
    }
}

@Test
void shouldSerializeWrites() throws Exception {
    // SQLite allows only one writer at a time
    // Verify second write waits rather than failing

    CountDownLatch latch1 = new CountDownLatch(1);
    CountDownLatch latch2 = new CountDownLatch(1);

    Thread writer1 = new Thread(() -> {
        tradeService.recordTrade(createTradeRequest());
        latch1.countDown();
    });

    Thread writer2 = new Thread(() -> {
        try {
            latch1.await(); // Wait for first write to start
            tradeService.recordTrade(createTradeRequest());
            latch2.countDown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    });

    writer1.start();
    writer2.start();

    // Both should complete successfully
    assertThat(latch2.await(10, TimeUnit.SECONDS)).isTrue();
}
```

### Financial Precision Tests

```java
@Test
void shouldMaintainBigDecimalPrecisionInCalculations() {
    // Test various edge cases
    assertQuoteAmount("1000000.00", "1.085000", "1085000.0000");
    assertQuoteAmount("999999.99", "1.085001", "1084999.9899");
    assertQuoteAmount("0.01", "1.5", "0.0150");
    assertQuoteAmount("1000000.00", "0.000001", "1.0000");
}

@Test
void shouldNotLosePrecisionInDatabaseRoundTrip() {
    BigDecimal original = new BigDecimal("1234567.8901");

    Trade trade = new Trade();
    trade.setBaseAmount(original);
    trade = tradeRepository.save(trade);

    Trade retrieved = tradeRepository.findById(trade.getId()).get();

    assertThat(retrieved.getBaseAmount()).isEqualByComparingTo(original);
}
```

### Validation Tests

```java
@Test
void shouldRejectInvalidCurrencyCodes() {
    Trade trade = createTrade();
    trade.setBaseCurrency("EURO"); // Should be "EUR"

    assertThatThrownBy(() -> tradeService.recordTrade(trade))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("currency");
}

@Test
void shouldRejectNegativeAmounts() {
    Trade trade = createTrade();
    trade.setBaseAmount(new BigDecimal("-1000"));

    assertThatThrownBy(() -> tradeService.recordTrade(trade))
        .isInstanceOf(ValidationException.class);
}

@Test
void shouldRejectValueDateBeforeTradeDate() {
    Trade trade = createTrade();
    trade.setTradeDate(LocalDate.of(2025, 10, 15));
    trade.setValueDate(LocalDate.of(2025, 10, 10));

    assertThatThrownBy(() -> tradeService.recordTrade(trade))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("value date");
}

@Test
void shouldEnforceMaxTradeAmount() {
    // From application.yml: max-trade-amount: 10000000
    Trade trade = createTrade();
    trade.setBaseAmount(new BigDecimal("10000001"));

    assertThatThrownBy(() -> tradeService.recordTrade(trade))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("maximum");
}
```

## Test Structure and Organization

### Test Categories (Tags)

```java
@Tag("unit")        // Fast, no Spring context, no database
@Tag("integration") // Full Spring Boot context
@Tag("sqlite")      // Tests specific to SQLite behavior
@Tag("slow")        // Tests that take >1 second
```

### Running Tests

```bash
# All tests
mvn test

# Fast tests only
mvn test -Dgroups="unit"

# Integration tests only
mvn test -Dgroups="integration"

# Everything including coverage
mvn verify
```

### Test Data Builders

Create fluent builders for readable test setup:

```java
public class TradeTestBuilder {
    private Trade trade = new Trade();

    public static TradeTestBuilder aTrade() {
        return new TradeTestBuilder()
            .withDefaults();
    }

    public TradeTestBuilder withDefaults() {
        trade.setTradeDate(LocalDate.now());
        trade.setValueDate(LocalDate.now().plusDays(2));
        trade.setDirection(Direction.BUY);
        trade.setBaseCurrency("EUR");
        trade.setQuoteCurrency("USD");
        trade.setBaseAmount(new BigDecimal("1000000"));
        trade.setExchangeRate(new BigDecimal("1.0850"));
        trade.setTrader("Test Trader");
        return this;
    }

    public TradeTestBuilder withEurUsd() {
        trade.setBaseCurrency("EUR");
        trade.setQuoteCurrency("USD");
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

    public Trade build() {
        return trade;
    }
}

// Usage in tests:
Trade trade = aTrade()
    .withEurUsd()
    .buyDirection()
    .withAmount("1000000")
    .withRate("1.0850")
    .build();
```

### Pending Tests

Use `@Disabled` to mark tests for features not yet implemented:

```java
@Disabled("TODO: Implement currency pair validation against allowed list")
@Test
void shouldRejectTradesWithDisallowedCurrencyPairs() {
    // Test implementation here
}
```

This shows intent and prevents forgetting edge cases.

## Coverage Requirements

Minimum coverage targets:

- **Service layer**: 80%
- **Domain logic**: 90%
- **Controllers**: 60%
- **Repositories**: 70%

Run coverage report:
```bash
mvn verify
# Report at: target/site/jacoco/index.html
```

## What NOT to Test

Don't waste time testing:

- **Lombok getters/setters** - Trust the library
- **Spring framework behavior** - Trust the framework
- **SQLite internals** - Trust the database
- **Jackson JSON serialization** - Trust the library (unless custom serializers)

Focus on **your business logic** and **integration points**.

## Test Configuration

### In-Memory SQLite for Tests

```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:sqlite:file::memory:?cache=shared
  jpa:
    hibernate:
      ddl-auto: validate
  liquibase:
    enabled: true
```

This gives fast, isolated tests with real SQLite behavior.

## Claude Code Warnings

### Watch for Test Modifications

Claude Code may occasionally **modify tests to make them pass** rather than fixing the actual bug.

**Red flags**:
- Tests suddenly pass after Claude Code changes without you reviewing
- Assertions weakened (e.g., `assertThat(x).isNotNull()` instead of `assertThat(x).isEqualTo(expected)`)
- Tests disabled or marked with `@Disabled`
- Expected exceptions removed
- Specific assertions replaced with generic ones

**Mitigation**:
1. Review all test changes carefully in diffs
2. Question why a test needed to change
3. Verify the feature actually works as intended
4. Consider test modifications as suspicious until proven innocent

## Testing Workflow for New Features

1. **Write failing test** - Red
2. **Implement feature** - Green
3. **Refactor** - Keep green
4. **Add audit verification** - For state changes
5. **Add edge case tests** - Negative cases
6. **Check coverage** - Ensure >80%
7. **Review with fresh eyes** - Did Claude cheat?

## Missing Tests in Current Codebase

The current implementation has **ZERO tests**. Priority tests needed:

1. ✗ Trade reference generation race condition
2. ✗ BigDecimal precision in calculations
3. ✗ Audit trail creation
4. ✗ SQLite concurrent reads/writes
5. ✗ Validation (currencies, amounts, dates)
6. ✗ Status transition rules
7. ✗ Repository queries
8. ✗ API endpoints
9. ✗ Security (authentication required)
10. ✗ Error handling

**Action required**: Implement comprehensive test suite before any production use.

## Summary

For a financial transaction system with weak operational support, testing is not optional. The test suite is your safety net.

**Key principles**:
- Test first, code second (TDD)
- Financial precision requires BigDecimal tests
- Audit trail must be verified in every state-changing test
- SQLite concurrency needs specific testing
- Watch for Claude Code modifying tests instead of fixing bugs
- Aim for >80% coverage on business logic

When in doubt, write another test.
