package com.woorifisa.won_card_channel_server.domain.sweep.model;

import com.woorifisa.won_card_channel_server.domain.sweep.dto.command.AutoSweepTarget;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepProcessStatus;
import com.woorifisa.won_card_channel_server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "card_chn_sweep_request",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_card_chn_sweep_request_point_ledger_id", columnNames = "point_ledger_id"),
                @UniqueConstraint(name = "uk_card_chn_sweep_request_idempotency_key", columnNames = "idempotency_key")
        },
        indexes = {
                @Index(name = "idx_card_chn_sweep_request_status", columnList = "request_status"),
                @Index(name = "idx_card_chn_sweep_request_correlation_id", columnList = "correlation_id"),
                @Index(name = "idx_card_chn_sweep_request_user_uuid", columnList = "user_uuid")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sweep extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sweep_request_id")
    private Long sweepRequestId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "user_uuid", nullable = false, length = 36)
    private UUID userUuid;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "card_user_uuid", nullable = false, length = 36)
    private UUID cardUserUuid;

    @Column(name = "performance_id", nullable = false)
    private Long performanceId;

    @Column(name = "point_ledger_id", nullable = false)
    private Long pointLedgerId;

    @Column(name = "base_month", nullable = false, length = 7)
    private String baseMonth;

    @Column(name = "point_amount", nullable = false)
    private Long pointAmount;

    @Column(name = "krw_amount", nullable = false)
    private Long krwAmount;

    @Column(name = "etf_id", nullable = false)
    private Long etfId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", nullable = false, length = 30)
    private SweepProcessStatus requestStatus;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "fail_reason", length = 500)
    private String failReason;

    @Builder(access = AccessLevel.PRIVATE)
    private Sweep(UUID userUuid, UUID cardUserUuid,
                  Long performanceId, Long pointLedgerId, String baseMonth, Long pointAmount, Long krwAmount, Long etfId,
                  SweepProcessStatus requestStatus, String correlationId, String idempotencyKey, LocalDateTime requestedAt
    ) {
        this.userUuid = userUuid;
        this.cardUserUuid = cardUserUuid;
        this.performanceId = performanceId;
        this.pointLedgerId = pointLedgerId;
        this.baseMonth = baseMonth;
        this.pointAmount = pointAmount;
        this.krwAmount = krwAmount;
        this.etfId = etfId;
        this.requestStatus = requestStatus;
        this.correlationId = correlationId;
        this.idempotencyKey = idempotencyKey;
        this.requestedAt = requestedAt;
    }

    public static Sweep createPendingPublish(
            AutoSweepTarget target, String correlationId, String idempotencyKey
    ) {
        return createPendingPublish(target, correlationId, idempotencyKey, LocalDateTime.now());
    }

    public static Sweep createPendingPublish(
            AutoSweepTarget target, String correlationId, String idempotencyKey, LocalDateTime requestedAt
    ) {
        return Sweep.builder()
                .userUuid(target.userUuid())
                .cardUserUuid(target.cardUserUuid())
                .performanceId(target.performanceId())
                .pointLedgerId(target.pointLedgerId())
                .baseMonth(target.baseMonth())
                .pointAmount(target.pointAmount())
                .krwAmount(target.krwAmount())
                .etfId(target.etfId())
                .requestStatus(SweepProcessStatus.PENDING_PUBLISH)
                .correlationId(correlationId)
                .idempotencyKey(idempotencyKey)
                .requestedAt(requestedAt == null ? LocalDateTime.now() : requestedAt)
                .build();
    }

    public void markSucceeded() {
        this.requestStatus = SweepProcessStatus.SUCCEEDED;
        this.completedAt = LocalDateTime.now();
        this.failReason = null;
    }

    public void markFailed(String failReason) {
        this.requestStatus = SweepProcessStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.failReason = truncate(failReason);
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }

        return value.length() > 500 ? value.substring(0, 500) : value;
    }

}
