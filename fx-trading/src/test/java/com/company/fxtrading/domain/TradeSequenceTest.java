package com.company.fxtrading.domain;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class TradeSequenceTest {

    @Test
    void shouldIncrementSequenceAtomically() {
        // Given
        TradeSequence sequence = new TradeSequence();
        sequence.setTradeDate(LocalDate.now());
        sequence.setNextSequence(1);
        sequence.setLastUpdated(LocalDateTime.now());

        // When
        Integer first = sequence.getAndIncrement();
        Integer second = sequence.getAndIncrement();
        Integer third = sequence.getAndIncrement();

        // Then
        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(2);
        assertThat(third).isEqualTo(3);
        assertThat(sequence.getNextSequence()).isEqualTo(4);
    }

    @Test
    void shouldUpdateLastUpdatedWhenIncrementing() {
        // Given
        TradeSequence sequence = new TradeSequence();
        sequence.setTradeDate(LocalDate.now());
        sequence.setNextSequence(1);
        LocalDateTime before = LocalDateTime.now().minusMinutes(1);
        sequence.setLastUpdated(before);

        // When
        sequence.getAndIncrement();

        // Then
        assertThat(sequence.getLastUpdated()).isAfter(before);
    }
}
