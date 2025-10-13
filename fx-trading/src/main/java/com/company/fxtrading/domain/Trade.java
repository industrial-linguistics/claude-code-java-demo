package com.company.fxtrading.domain;

import com.company.fxtrading.validation.ValidCurrency;
import com.company.fxtrading.validation.ValidTradeDates;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Data
@ValidTradeDates
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String tradeReference;

    @NotNull(message = "Trade date is required")
    @Column(nullable = false)
    private LocalDate tradeDate;

    @NotNull(message = "Value date is required")
    @Column(nullable = false)
    private LocalDate valueDate;

    @NotNull(message = "Direction is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private Direction direction;

    @NotNull(message = "Base currency is required")
    @ValidCurrency
    @Column(nullable = false, length = 3)
    private String baseCurrency;

    @NotNull(message = "Quote currency is required")
    @ValidCurrency
    @Column(nullable = false, length = 3)
    private String quoteCurrency;

    @NotNull(message = "Base amount is required")
    @DecimalMin(value = "0.01", message = "Base amount must be at least 0.01")
    @DecimalMax(value = "10000000", message = "Base amount cannot exceed 10,000,000")
    @Digits(integer = 15, fraction = 4, message = "Base amount has invalid precision")
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal baseAmount;

    @NotNull(message = "Exchange rate is required")
    @DecimalMin(value = "0.000001", message = "Exchange rate must be positive")
    @DecimalMax(value = "1000000", message = "Exchange rate cannot exceed 1,000,000")
    @Digits(integer = 9, fraction = 6, message = "Exchange rate has invalid precision")
    @Column(nullable = false, precision = 15, scale = 6)
    private BigDecimal exchangeRate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quoteAmount;

    @Size(max = 100, message = "Counterparty name cannot exceed 100 characters")
    @Column(length = 100)
    private String counterparty;

    @Size(max = 50, message = "Trader name cannot exceed 50 characters")
    @Column(length = 50)
    private String trader;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TradeStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private String updatedBy;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = TradeStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
