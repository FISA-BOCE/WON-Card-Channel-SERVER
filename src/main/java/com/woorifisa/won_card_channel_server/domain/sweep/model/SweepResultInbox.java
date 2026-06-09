package com.woorifisa.won_card_channel_server.domain.sweep.model;

import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.InboxProcessStatus;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepEventType;
import com.woorifisa.won_card_channel_server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "card_chn_inbox_event",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_card_chn_inbox_source_event_id", columnNames = "source_event_id"),
                @UniqueConstraint(name = "uk_card_chn_inbox_idempotency_key", columnNames = "idempotency_key")
        },
        indexes = {
                @Index(name = "idx_card_chn_inbox_process_status", columnList = "process_status"),
                @Index(name = "idx_card_chn_inbox_correlation_id", columnList = "correlation_id"),
                @Index(name = "idx_card_chn_inbox_sweep_request_id", columnList = "sweep_request_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SweepResultInbox extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inbox_event_id")
    private Long inboxEventId;

    @Column(name = "source_event_id", nullable = false, length = 100)
    private String sourceEventId;

    @Column(name = "sweep_request_id", nullable = false)
    private Long sweepRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private SweepEventType eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "json")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "process_status", nullable = false, length = 30)
    private InboxProcessStatus processStatus;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "last_error_message", length = 500)
    private String lastErrorMessage;

    private SweepResultInbox(
            String sourceEventId,
            Long sweepRequestId,
            SweepEventType eventType,
            String payload,
            String correlationId,
            String idempotencyKey
    ) {
        this.sourceEventId = sourceEventId;
        this.sweepRequestId = sweepRequestId;
        this.eventType = eventType;
        this.payload = payload;
        this.correlationId = correlationId;
        this.idempotencyKey = idempotencyKey;
        this.processStatus = InboxProcessStatus.RECEIVED;
        this.retryCount = 0;
        this.receivedAt = LocalDateTime.now();
    }

    public static SweepResultInbox received(
            String sourceEventId,
            Long sweepRequestId,
            SweepEventType eventType,
            String payload,
            String correlationId,
            String idempotencyKey
    ) {
        return new SweepResultInbox(sourceEventId, sweepRequestId, eventType, payload, correlationId, idempotencyKey);
    }

    public void markProcessing() {
        this.processStatus = InboxProcessStatus.PROCESSING;
        this.lastErrorMessage = null;
    }

    public void markProcessed() {
        this.processStatus = InboxProcessStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
        this.lastErrorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.processStatus = InboxProcessStatus.FAILED;
        this.retryCount++;
        this.lastErrorMessage = truncate(errorMessage);
    }

    public boolean alreadyProcessed() {
        return this.processStatus == InboxProcessStatus.PROCESSED;
    }

    public boolean canClaim(LocalDateTime processingTimeoutAt) {
        return this.processStatus == InboxProcessStatus.RECEIVED
                || this.processStatus == InboxProcessStatus.FAILED
                || (this.processStatus == InboxProcessStatus.PROCESSING
                && this.getUpdatedAt().isBefore(processingTimeoutAt));
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }

        return value.length() > 500 ? value.substring(0, 500) : value;
    }
}
