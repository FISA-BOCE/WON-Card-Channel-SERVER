package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.woorifisa.won_card_channel_server.domain.sweep.model.CardChnSweepOutbox;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.OutboxPublishStatus;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepEventType;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.CardChnSweepOutboxRepository;
import com.woorifisa.won_card_channel_server.global.config.SqsProperties;
import com.woorifisa.won_card_channel_server.global.config.SweepOutboxPublisherProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SweepOutboxPublishServiceTest {

    private CardChnSweepOutboxRepository outboxRepository;
    private SqsClient sqsClient;
    private SqsProperties sqsProperties;
    private SweepOutboxPublisherProperties publisherProperties;
    private SweepOutboxPublishService publishService;

    @BeforeEach
    void setUp() {
        outboxRepository = mock(CardChnSweepOutboxRepository.class);
        sqsClient = mock(SqsClient.class);
        sqsProperties = new SqsProperties(
                "ap-northeast-2",
                "http://localhost:4566",
                "http://localhost:4566/000000000000/won-card-sweep-request-queue.fifo"
        );
        publisherProperties = new SweepOutboxPublisherProperties(
                true,
                20,
                3,
                10000L
        );

        publishService = new SweepOutboxPublishService(
                outboxRepository,
                sqsClient,
                sqsProperties,
                publisherProperties
        );
    }

    @Test
    @DisplayName("SQS 발행에 성공하면 Outbox 상태를 PUBLISHED로 변경한다")
    void publishSuccess() {
        // given
        CardChnSweepOutbox outbox = createPendingOutbox();

        when(outboxRepository.findById(1L)).thenReturn(Optional.of(outbox));
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().messageId("message-1").build());

        // when
        publishService.publish(1L);

        // then
        assertThat(outbox.getPublishStatus()).isEqualTo(OutboxPublishStatus.PUBLISHED);
        assertThat(outbox.getPublishedAt()).isNotNull();
        assertThat(outbox.getLastErrorMessage()).isNull();

        ArgumentCaptor<SendMessageRequest> captor =
                ArgumentCaptor.forClass(SendMessageRequest.class);

        verify(sqsClient).sendMessage(captor.capture());

        SendMessageRequest request = captor.getValue();

        assertThat(request.queueUrl()).isEqualTo("http://localhost:4566/000000000000/won-card-sweep-request-queue.fifo");
        assertThat(request.messageBody()).contains("\"eventType\":\"SWEEP_REQUESTED\"");
        assertThat(request.messageDeduplicationId()).isEqualTo("SWEEP:POINT_LEDGER:1");
        assertThat(request.messageGroupId()).isEqualTo("SWEEP_REQUESTED");
    }

    @Test
    @DisplayName("SQS 발행에 실패하고 최대 재시도 전이면 RETRY 상태로 변경한다")
    void publishFailThenRetry() {
        // given
        CardChnSweepOutbox outbox = createPendingOutbox();

        when(outboxRepository.findById(1L)).thenReturn(Optional.of(outbox));
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(new RuntimeException("SQS timeout"));

        // when
        publishService.publish(1L);

        // then
        assertThat(outbox.getPublishStatus()).isEqualTo(OutboxPublishStatus.RETRY);
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        assertThat(outbox.getLastErrorMessage()).contains("SQS timeout");
        assertThat(outbox.getNextRetryAt()).isNotNull();
    }

    @Test
    @DisplayName("SQS 발행 실패 횟수가 최대 재시도 횟수에 도달하면 FAILED 상태로 변경한다")
    void publishFailThenFailedWhenMaxRetryReached() {
        // given
        CardChnSweepOutbox outbox = createPendingOutbox();
        outbox.markRetry("first");
        outbox.markRetry("second");

        when(outboxRepository.findById(1L)).thenReturn(Optional.of(outbox));
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(new RuntimeException("SQS timeout"));

        // when
        publishService.publish(1L);

        // then
        assertThat(outbox.getPublishStatus()).isEqualTo(OutboxPublishStatus.FAILED);
        assertThat(outbox.getRetryCount()).isEqualTo(3);
        assertThat(outbox.getLastErrorMessage()).contains("SQS timeout");
        assertThat(outbox.getNextRetryAt()).isNull();
    }

    private CardChnSweepOutbox createPendingOutbox() {
        return CardChnSweepOutbox.pending(
                2L,
                "CARD-SWEEP-988351d5-6242-4299-86ef-465cd9809874",
                SweepEventType.SWEEP_REQUESTED,
                """
                        {"eventType":"SWEEP_REQUESTED","pointLedgerId":1,"etfId":100}
                        """,
                "correlation-id",
                "SWEEP:POINT_LEDGER:1"
        );
    }
}
