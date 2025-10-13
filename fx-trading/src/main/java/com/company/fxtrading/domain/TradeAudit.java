package com.company.fxtrading.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_audit")
@Data
public class TradeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tradeId;

    @Column(nullable = false, length = 50)
    private String tradeReference;

    @Column(nullable = false)
    private LocalDateTime auditTimestamp;

    @Column(nullable = false, length = 50)
    private String auditUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditAction action;

    @Column(columnDefinition = "TEXT")
    private String changeDetails;

    @Column(columnDefinition = "TEXT")
    private String beforeSnapshot;

    @Column(columnDefinition = "TEXT")
    private String afterSnapshot;
}
