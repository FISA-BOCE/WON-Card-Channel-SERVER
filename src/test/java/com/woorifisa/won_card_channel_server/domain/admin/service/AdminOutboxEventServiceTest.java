package com.woorifisa.won_card_channel_server.domain.admin.service;

import com.woorifisa.won_card_channel_server.domain.sweep.exception.code.SweepErrorCode;
import com.woorifisa.won_card_channel_server.domain.sweep.model.SweepOutbox;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.OutboxPublishStatus;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepEventType;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.SweepOutboxRepository;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminOutboxEventServiceTest {

    private SweepOutboxRepository sweepOutboxRepository;
    private AdminOutboxEventService service;

    @BeforeEach
    void setUp() {
        sweepOutboxRepository = mock(SweepOutboxRepository.class);
        service = new AdminOutboxEventService(sweepOutboxRepository);
    }

    @Test
    @DisplayName("SQS 발행 실패로 FAILED 된 Outbox 이벤트를 RETRY 상태로 되돌린다")
    void retryFailedOutboxEvent() {
        SweepOutbox outbox = createOutbox();
        outbox.markProcessing();
        outbox.markFailed("QueueDoesNotExistException: The specified queue does not exist.");

        when(sweepOutboxRepository.findById(1L)).thenReturn(Optional.of(outbox));

        var response = service.retryOutboxEvent(1L);

        assertThat(outbox.getPublishStatus()).isEqualTo(OutboxPublishStatus.RETRY);
        assertThat(outbox.getNextRetryAt()).isNotNull();
        assertThat(response.retryable()).isTrue();
        assertThat(response.retryDisabledReason()).isNull();
    }

    @Test
    @DisplayName("이미 발행 완료된 Outbox 이벤트는 수동 재처리할 수 없다")
    void retryPublishedOutboxEventFails() {
        SweepOutbox outbox = createOutbox();
        outbox.markProcessing();
        outbox.markPublished();

        when(sweepOutboxRepository.findById(1L)).thenReturn(Optional.of(outbox));

        assertThatThrownBy(() -> service.retryOutboxEvent(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", SweepErrorCode.SWEEP_OUTBOX_RETRY_NOT_ALLOWED);
    }

    private SweepOutbox createOutbox() {
        return SweepOutbox.pending(
                2L,
                "CARD-SWEEP-1",
                SweepEventType.SWEEP_REQUESTED,
                "{\"eventType\":\"SWEEP_REQUESTED\",\"pointLedgerId\":1,\"etfId\":100}",
                "correlation-id",
                "SWEEP:POINT_LEDGER:1"
        );
    }
}
