package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.woorifisa.won_card_channel_server.domain.sweep.dto.command.SweepOutboxPublishMessage;
import com.woorifisa.won_card_channel_server.domain.sweep.exception.code.SweepErrorCode;
import com.woorifisa.won_card_channel_server.global.config.SqsProperties;
import com.woorifisa.won_card_channel_server.global.config.SweepOutboxPublisherProperties;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SweepOutboxPublishServiceTest {

    @Mock
    private SweepOutboxStatusService statusService;

    @Mock
    private SqsClient sqsClient;

    private SqsProperties sqsProperties;
    private SweepOutboxPublisherProperties publisherProperties;
    private SweepOutboxPublishService publishService;

    @BeforeEach
    void setUp() {
        sqsProperties = new SqsProperties(
                "ap-northeast-2",
                "http://localhost:4566",
                "http://localhost:4566/000000000000/won-card-sweep-request-queue.fifo",
                "http://localhost:4566/000000000000/won-invest-sweep-result-queue.fifo"

        );

        publisherProperties = new SweepOutboxPublisherProperties(
                true,
                20,
                3,
                10000L,
                64
        );

        publishService = new SweepOutboxPublishService(
                statusService,
                sqsClient,
                sqsProperties,
                publisherProperties
        );
    }

    @Test
    @DisplayName("SQS 발행에 성공하면 Outbox 상태를 PUBLISHED로 변경한다")
    void publishSuccess() {
        // given
        SweepOutboxPublishMessage message = createPublishMessage();

        when(statusService.getPublishMessage(1L)).thenReturn(message);
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().messageId("message-1").build());

        // when
        publishService.publish(1L);

        // then
        ArgumentCaptor<SendMessageRequest> requestCaptor =
                ArgumentCaptor.forClass(SendMessageRequest.class);

        verify(sqsClient).sendMessage(requestCaptor.capture());
        verify(statusService).markPublished(1L);
        verify(statusService, never()).markPublishFailed(any(), any(), anyInt());

        SendMessageRequest request = requestCaptor.getValue();

        assertThat(request.queueUrl())
                .isEqualTo("http://localhost:4566/000000000000/won-card-sweep-request-queue.fifo");
        assertThat(request.messageBody()).contains("\"eventType\":\"SWEEP_REQUESTED\"");
        assertThat(request.messageDeduplicationId()).isEqualTo("SWEEP:POINT_LEDGER:1");
        assertThat(request.messageGroupId()).startsWith("sweep-user-");
    }

    @Test
    @DisplayName("SQS 발행에 실패하면 Outbox 실패 상태를 반영한다")
    void publishFail() {
        // given
        SweepOutboxPublishMessage message = createPublishMessage();

        when(statusService.getPublishMessage(1L)).thenReturn(message);
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(new RuntimeException("SQS timeout"));

        // when
        publishService.publish(1L);

        // then
        verify(statusService, never()).markPublished(any());
        verify(statusService).markPublishFailed(
                eq(1L),
                contains("SQS timeout"),
                eq(3)
        );
    }

    @Test
    @DisplayName("발행 메시지 조회에 실패하면 SQS 발행과 상태 변경을 수행하지 않는다")
    void publishSkipWhenGetPublishMessageFails() {
        // given
        when(statusService.getPublishMessage(1L))
                .thenThrow(new BusinessException(SweepErrorCode.SWEEP_OUTBOX_INVALID_PUBLISH_STATE));

        // when
        publishService.publish(1L);

        // then
        verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
        verify(statusService, never()).markPublished(any());
        verify(statusService, never()).markPublishFailed(any(), any(), anyInt());
    }

    private SweepOutboxPublishMessage createPublishMessage() {
        return new SweepOutboxPublishMessage(
                1L,
                2L,
                "CARD-SWEEP-988351d5-6242-4299-86ef-465cd9809874",
                "{\"eventType\":\"SWEEP_REQUESTED\",\"pointLedgerId\":1,\"etfId\":100}",
                "SWEEP:POINT_LEDGER:1",
                UUID.fromString("22222222-2222-2222-2222-222222222222")
        );
    }

}
