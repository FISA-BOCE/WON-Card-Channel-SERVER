package com.woorifisa.won_card_channel_server.domain.sweep.model;

import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.OutboxPublishStatus;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepEventType;
import com.woorifisa.won_card_channel_server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "card_chn_sweep_outbox",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_card_chn_sweep_outbox_event_id", columnNames = "event_id"),
                @UniqueConstraint(name = "uk_card_chn_sweep_outbox_event_type_idempotency_key", columnNames = {"event_type", "idempotency_key"})
        },
        indexes = {
                @Index(name = "idx_card_chn_sweep_outbox_publish_status", columnList = "publish_status"),
                @Index(name = "idx_card_chn_sweep_outbox_sweep_request_id", columnList = "sweep_request_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardChnSweepOutbox extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_event_id")
    private Long outboxEventId;

    @Column(name = "sweep_request_id", nullable = false)
    private Long sweepRequestId;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private SweepEventType eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "publish_status", nullable = false, length = 30)
    private OutboxPublishStatus publishStatus;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "last_error_message", length = 1000)
    private String lastErrorMessage;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private CardChnSweepOutbox(
            Long sweepRequestId, String eventId, SweepEventType eventType, String payload,
            OutboxPublishStatus publishStatus, String correlationId, String idempotencyKey,
            int retryCount, LocalDateTime nextRetryAt
    ) {
        this.sweepRequestId = sweepRequestId;
        this.eventId = eventId;
        this.eventType = eventType;
        this.payload = payload;
        this.publishStatus = publishStatus;
        this.correlationId = correlationId;
        this.idempotencyKey = idempotencyKey;
        this.retryCount = retryCount;
        this.nextRetryAt = nextRetryAt;
    }

    public static CardChnSweepOutbox pending(
            Long sweepRequestId, String eventId, SweepEventType eventType, String payload,
            String correlationId, String idempotencyKey
    ) {
        return CardChnSweepOutbox.builder()
                .sweepRequestId(sweepRequestId)
                .eventId(eventId)
                .eventType(eventType)
                .payload(payload)
                .publishStatus(OutboxPublishStatus.PENDING)
                .correlationId(correlationId)
                .idempotencyKey(idempotencyKey)
                .retryCount(0)
                .nextRetryAt(LocalDateTime.now())
                .build();
    }
}
