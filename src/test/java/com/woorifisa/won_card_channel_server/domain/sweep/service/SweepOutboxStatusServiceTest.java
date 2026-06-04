package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.woorifisa.won_card_channel_server.domain.sweep.dto.command.SweepOutboxPublishMessage;
import com.woorifisa.won_card_channel_server.domain.sweep.exception.code.SweepErrorCode;
import com.woorifisa.won_card_channel_server.domain.sweep.model.Sweep;
import com.woorifisa.won_card_channel_server.domain.sweep.model.SweepOutbox;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.OutboxPublishStatus;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepEventType;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.SweepOutboxRepository;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.SweepRepository;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SweepOutboxStatusServiceTest {

    @Mock
    private SweepOutboxRepository outboxRepository;

    @Mock
    private SweepRepository sweepRepository;

    private SweepOutboxStatusService statusService;

    @BeforeEach
    void setUp() {
        statusService = new SweepOutboxStatusService(outboxRepository, sweepRepository);
    }

    @Test
    @DisplayName("PROCESSING 상태 Outbox이면 발행 메시지를 반환한다")
    void getPublishMessageSuccess() {
        // given
        SweepOutbox outbox = createPendingOutbox();
        setField(outbox, "outboxEventId", 1L);
        outbox.markProcessing();

        when(outboxRepository.findById(1L)).thenReturn(Optional.of(outbox));
        Sweep sweep = mock(Sweep.class);
        when(sweep.getCardUserUuid()).thenReturn(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        when(sweepRepository.findById(2L)).thenReturn(Optional.of(sweep));

        // when
        SweepOutboxPublishMessage message = statusService.getPublishMessage(1L);

        // then
        assertThat(message.outboxEventId()).isEqualTo(1L);
        assertThat(message.sweepRequestId()).isEqualTo(2L);
        assertThat(message.eventId()).isEqualTo("CARD-SWEEP-1");
        assertThat(message.payload()).contains("\"eventType\":\"SWEEP_REQUESTED\"");
        assertThat(message.idempotencyKey()).isEqualTo("SWEEP:POINT_LEDGER:1");
        assertThat(message.cardUserUuid()).isEqualTo(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    }

    @Test
    @DisplayName("PROCESSING 상태가 아니면 발행 메시지를 반환하지 않는다")
    void getPublishMessageInvalidState() {
        // given
        SweepOutbox outbox = createPendingOutbox();
        setField(outbox, "outboxEventId", 1L);

        when(outboxRepository.findById(1L)).thenReturn(Optional.of(outbox));

        // when & then
        assertThatThrownBy(() -> statusService.getPublishMessage(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode",
                        SweepErrorCode.SWEEP_OUTBOX_INVALID_PUBLISH_STATE
                );
    }

    @Test
    @DisplayName("Outbox가 없으면 예외가 발생한다")
    void getPublishMessageOutboxNotFound() {
        // given
        when(outboxRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> statusService.getPublishMessage(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode",
                        SweepErrorCode.SWEEP_OUTBOX_NOT_FOUND
                );
    }

    @Test
    @DisplayName("PROCESSING 상태 Outbox를 PUBLISHED 상태로 변경한다")
    void markPublishedSuccess() {
        // given
        SweepOutbox outbox = createPendingOutbox();
        setField(outbox, "outboxEventId", 1L);
        outbox.markProcessing();

        when(outboxRepository.findById(1L)).thenReturn(Optional.of(outbox));

        // when
        statusService.markPublished(1L);

        // then
        assertThat(outbox.getPublishStatus()).isEqualTo(OutboxPublishStatus.PUBLISHED);
        assertThat(outbox.getPublishedAt()).isNotNull();
        assertThat(outbox.getLastErrorMessage()).isNull();
    }

    @Test
    @DisplayName("PROCESSING 상태가 아니면 PUBLISHED 상태 변경을 건너뛴다")
    void markPublishedSkipWhenNotProcessing() {
        // given
        SweepOutbox outbox = createPendingOutbox();
        setField(outbox, "outboxEventId", 1L);

        when(outboxRepository.findById(1L)).thenReturn(Optional.of(outbox));

        // when
        statusService.markPublished(1L);

        // then
        assertThat(outbox.getPublishStatus()).isEqualTo(OutboxPublishStatus.PENDING);
        assertThat(outbox.getPublishedAt()).isNull();
    }

    @Test
    @DisplayName("PROCESSING 상태 Outbox 발행 실패 시 RETRY 상태로 변경한다")
    void markPublishFailedRetry() {
        // given
        SweepOutbox outbox = createPendingOutbox();
        setField(outbox, "outboxEventId", 1L);
        outbox.markProcessing();

        when(outboxRepository.findById(1L)).thenReturn(Optional.of(outbox));

        // when
        statusService.markPublishFailed(1L, "SQS timeout", 3);

        // then
        assertThat(outbox.getPublishStatus()).isEqualTo(OutboxPublishStatus.RETRY);
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        assertThat(outbox.getLastErrorMessage()).contains("SQS timeout");
        assertThat(outbox.getNextRetryAt()).isNotNull();
    }

    @Test
    @DisplayName("최대 재시도 횟수에 도달하면 FAILED 상태로 변경한다")
    void markPublishFailedMaxRetryReached() {
        // given
        SweepOutbox outbox = createPendingOutbox();
        setField(outbox, "outboxEventId", 1L);
        outbox.markProcessing();

        // retryCount를 2로 맞춰서 maxRetryCount=3에서 이번 실패 후 FAILED가 되도록 설정
        setField(outbox, "retryCount", 2);

        when(outboxRepository.findById(1L)).thenReturn(Optional.of(outbox));

        // when
        statusService.markPublishFailed(1L, "SQS timeout", 3);

        // then
        assertThat(outbox.getPublishStatus()).isEqualTo(OutboxPublishStatus.FAILED);
        assertThat(outbox.getRetryCount()).isEqualTo(3);
        assertThat(outbox.getLastErrorMessage()).contains("SQS timeout");
        assertThat(outbox.getNextRetryAt()).isNull();
    }

    @Test
    @DisplayName("PROCESSING 상태가 아니면 발행 실패 상태 변경을 건너뛴다")
    void markPublishFailedSkipWhenNotProcessing() {
        // given
        SweepOutbox outbox = createPendingOutbox();
        setField(outbox, "outboxEventId", 1L);

        when(outboxRepository.findById(1L)).thenReturn(Optional.of(outbox));

        // when
        statusService.markPublishFailed(1L, "SQS timeout", 3);

        // then
        assertThat(outbox.getPublishStatus()).isEqualTo(OutboxPublishStatus.PENDING);
        assertThat(outbox.getRetryCount()).isEqualTo(0);
        assertThat(outbox.getLastErrorMessage()).isNull();
    }

    private SweepOutbox createPendingOutbox() {
        return SweepOutbox.pending(
                2L,
                "CARD-SWEEP-1",
                SweepEventType.SWEEP_REQUESTED,
                "{\"eventType\":\"SWEEP_REQUESTED\",\"pointLedgerId\":1,\"etfId\":100}",
                "correlation-id",
                "SWEEP:POINT_LEDGER:1"
        );
    }

    private void setField(Object target, String fieldName, Object value) {
        org.springframework.test.util.ReflectionTestUtils.setField(target, fieldName, value);
    }
}
