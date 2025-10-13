package com.company.fxtrading.service;

import com.company.fxtrading.TradeTestBuilder;
import com.company.fxtrading.domain.Trade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Critical concurrency tests for TradeService.
 * These tests expose race conditions and SQLite concurrency behavior.
 *
 * WARNING: These tests may fail due to the known race condition in
 * TradeService.generateTradeReference() (see TESTING.md:296-331)
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("integration")
@Tag("slow")
class TradeConcurrencyTest {

    @Autowired
    private TradeService tradeService;

    /**
     * CRITICAL TEST: Exposes the race condition in trade reference generation.
     *
     * The bug is in TradeService.generateTradeReference():
     * <pre>
     * long countToday = tradeRepository.countTradesByDate(LocalDate.now());
     * return String.format("FX-%s-%04d", datePrefix, countToday + 1);
     * </pre>
     *
     * Two threads can both read count=0, then both generate FX-20251012-0001,
     * causing a unique constraint violation.
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldGenerateUniqueTradeReferencesUnderConcurrentLoad() throws Exception {
        // Given
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        Set<String> references = ConcurrentHashMap.newKeySet();
        List<Exception> exceptions = new CopyOnWriteArrayList<>();

        // When - Create trades concurrently
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    Trade trade = TradeTestBuilder.aTrade().build();
                    Trade saved = tradeService.recordTrade(trade);
                    references.add(saved.getTradeReference());
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }));
        }

        // Wait for all threads
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Then - All references must be unique
        // This test will FAIL if the race condition exists
        if (!exceptions.isEmpty()) {
            System.err.println("Exceptions occurred during concurrent trade creation:");
            exceptions.forEach(e -> e.printStackTrace());
        }

        assertThat(exceptions)
            .withFailMessage("Race condition detected: duplicate trade references generated")
            .isEmpty();
        assertThat(references)
            .withFailMessage("Expected %d unique references but got %d", threadCount, references.size())
            .hasSize(threadCount);
    }

    /**
     * Test SQLite's ability to handle concurrent reads.
     * SQLite with WAL mode should handle this well.
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldHandleMultipleConcurrentReads() throws Exception {
        // Given - Create some test data
        for (int i = 0; i < 5; i++) {
            Trade trade = TradeTestBuilder.aTrade().build();
            tradeService.recordTrade(trade);
        }

        // When - 12 readers reading simultaneously (matching your actual use case)
        int readerCount = 12;
        ExecutorService executor = Executors.newFixedThreadPool(readerCount);
        List<Future<List<Trade>>> futures = new ArrayList<>();

        for (int i = 0; i < readerCount; i++) {
            futures.add(executor.submit(() -> tradeService.findAllTrades()));
        }

        // Then - All reads should succeed without timeout
        for (Future<List<Trade>> future : futures) {
            List<Trade> trades = future.get(10, TimeUnit.SECONDS);
            assertThat(trades).isNotNull();
            assertThat(trades).hasSizeGreaterThanOrEqualTo(5);
        }

        executor.shutdown();
    }

    /**
     * Test SQLite's write serialization.
     * SQLite allows only one writer at a time - second write should wait, not fail.
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldSerializeWrites() throws Exception {
        // Given
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch writer1Started = new CountDownLatch(1);

        List<Trade> results = new CopyOnWriteArrayList<>();
        List<Exception> exceptions = new CopyOnWriteArrayList<>();

        // When - Start two writers using executor service
        Future<?> future1 = executor.submit(() -> {
            try {
                startLatch.await(); // Wait for signal to start
                writer1Started.countDown(); // Signal that we started
                Trade trade = TradeTestBuilder.aTrade().build();
                Trade saved = tradeService.recordTrade(trade);
                results.add(saved);
            } catch (Exception e) {
                exceptions.add(e);
            }
        });

        Future<?> future2 = executor.submit(() -> {
            try {
                writer1Started.await(); // Wait for writer1 to start
                Trade trade = TradeTestBuilder.aTrade().build();
                Trade saved = tradeService.recordTrade(trade);
                results.add(saved);
            } catch (Exception e) {
                exceptions.add(e);
            }
        });

        startLatch.countDown(); // Start both writers

        // Then - Both should complete successfully (writes serialized)
        future1.get(15, TimeUnit.SECONDS);
        future2.get(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(exceptions).isEmpty();
        assertThat(results).hasSize(2);

        // Verify both trades were saved with unique references
        assertThat(results.get(0).getTradeReference())
            .isNotEqualTo(results.get(1).getTradeReference());
    }

    /**
     * Stress test: Many concurrent operations mixing reads and writes.
     * This simulates a realistic load scenario.
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldHandleMixedConcurrentReadsAndWrites() throws Exception {
        // Given - Create initial data
        for (int i = 0; i < 3; i++) {
            Trade trade = TradeTestBuilder.aTrade().build();
            tradeService.recordTrade(trade);
        }

        // When - Mix of readers and writers
        int readerCount = 8;
        int writerCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(readerCount + writerCount);

        List<Future<?>> futures = new ArrayList<>();
        List<Exception> exceptions = new CopyOnWriteArrayList<>();

        // Start readers
        for (int i = 0; i < readerCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    tradeService.findAllTrades();
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }));
        }

        // Start writers
        for (int i = 0; i < writerCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    Trade trade = TradeTestBuilder.aTrade().build();
                    tradeService.recordTrade(trade);
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }));
        }

        // Then - All operations should complete without errors
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }

        executor.shutdown();

        assertThat(exceptions)
            .withFailMessage("Exceptions occurred during mixed concurrent operations")
            .isEmpty();
    }

    /**
     * Test that demonstrates the EXACT race condition scenario.
     * This is a simplified version that makes the race condition more obvious.
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldDemonstratTradeReferenceRaceCondition() throws Exception {
        // Given - Two threads trying to create trades at the exact same time
        CyclicBarrier barrier = new CyclicBarrier(2); // Synchronize threads
        List<Trade> results = new CopyOnWriteArrayList<>();
        List<Exception> exceptions = new CopyOnWriteArrayList<>();

        Runnable createTrade = () -> {
            try {
                barrier.await(); // Wait for both threads to be ready
                // Both threads will now execute at the same time
                Trade trade = TradeTestBuilder.aTrade().build();
                Trade saved = tradeService.recordTrade(trade);
                results.add(saved);
            } catch (Exception e) {
                exceptions.add(e);
            }
        };

        // When - Execute simultaneously
        Thread thread1 = new Thread(createTrade);
        Thread thread2 = new Thread(createTrade);

        thread1.start();
        thread2.start();

        thread1.join(10000);
        thread2.join(10000);

        // Then - Should have 2 trades with different references
        // If race condition exists, one thread may fail or both may get same reference
        if (exceptions.isEmpty()) {
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getTradeReference())
                .isNotEqualTo(results.get(1).getTradeReference());
        } else {
            // Race condition detected via exception
            System.err.println("Race condition manifested as exception:");
            exceptions.forEach(e -> e.printStackTrace());
            assertThat(exceptions).isEmpty(); // Fail the test
        }
    }
}
