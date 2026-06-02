package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.woorifisa.won_card_channel_server.domain.sweep.dto.event.SweepInvestmentResultEvent;
import com.woorifisa.won_card_channel_server.domain.sweep.exception.code.SweepErrorCode;
import com.woorifisa.won_card_channel_server.domain.sweep.external.CardCoreRewardSweepApi;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepResultRequest;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepResultResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepEventType;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SweepResultProcessServiceTest {

    private CardCoreRewardSweepApi cardCoreRewardSweepApi;
    private SweepStatusUpdateService sweepStatusUpdateService;
    private SweepResultProcessService service;

    private final UUID userUuid = UUID.fromString("a5324ba5-0ee3-44c6-b3d5-a951f9e94df5");
    private final UUID cardUserUuid = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        cardCoreRewardSweepApi = mock(CardCoreRewardSweepApi.class);
        sweepStatusUpdateService = mock(SweepStatusUpdateService.class);
        service = new SweepResultProcessService(cardCoreRewardSweepApi, sweepStatusUpdateService);
    }

    @Test
    @DisplayName("투자 완료 결과는 채널 상태를 먼저 변경한 뒤 Card Core에 COMPLETED로 반영한다")
    void processCompleted() {
        SweepInvestmentResultEvent event = completedEvent();

        when(cardCoreRewardSweepApi.applySweepResult(any(), any(), any()))
                .thenReturn(ApiResponse.of(SuccessStatus.OK, new CardCoreSweepResultResponse(1L, "COMPLETED")));

        service.process(event);

        InOrder inOrder = inOrder(sweepStatusUpdateService, cardCoreRewardSweepApi);
        inOrder.verify(sweepStatusUpdateService).update(event);

        ArgumentCaptor<CardCoreSweepResultRequest> captor =
                ArgumentCaptor.forClass(CardCoreSweepResultRequest.class);

        inOrder.verify(cardCoreRewardSweepApi).applySweepResult(eq(cardUserUuid), eq(1L), captor.capture());

        assertThat(captor.getValue().sweepRequestId()).isEqualTo(2L);
        assertThat(captor.getValue().sweepExecutionId()).isEqualTo(1L);
        assertThat(captor.getValue().correlationId()).isEqualTo("CORR-SWEEP-TEST-1");
        assertThat(captor.getValue().idempotencyKey()).isEqualTo("SWEEP:POINT_LEDGER:1");
        assertThat(captor.getValue().resultStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("투자 실패 결과는 채널 상태를 먼저 변경한 뒤 Card Core에 FAILED로 반영한다")
    void processFailed() {
        SweepInvestmentResultEvent event = failedEvent();

        when(cardCoreRewardSweepApi.applySweepResult(any(), any(), any()))
                .thenReturn(ApiResponse.of(SuccessStatus.OK, new CardCoreSweepResultResponse(1L, "FAILED")));

        service.process(event);

        InOrder inOrder = inOrder(sweepStatusUpdateService, cardCoreRewardSweepApi);
        inOrder.verify(sweepStatusUpdateService).update(event);

        ArgumentCaptor<CardCoreSweepResultRequest> captor =
                ArgumentCaptor.forClass(CardCoreSweepResultRequest.class);

        inOrder.verify(cardCoreRewardSweepApi).applySweepResult(eq(cardUserUuid), eq(1L), captor.capture());

        assertThat(captor.getValue().resultStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("채널 상태 변경이 실패하면 Card Core를 호출하지 않는다")
    void processWhenChannelStatusUpdateFails() {
        SweepInvestmentResultEvent event = completedEvent();

        doThrow(new BusinessException(SweepErrorCode.SWEEP_REWARD_LEDGER_NOT_FOUND))
                .when(sweepStatusUpdateService)
                .update(event);

        assertThrows(BusinessException.class, () -> service.process(event));

        verify(cardCoreRewardSweepApi, never()).applySweepResult(any(), any(), any());
    }

    @Test
    @DisplayName("필수값이 없는 결과 이벤트는 검증에 실패한다")
    void validateInvalidEvent() {
        SweepInvestmentResultEvent event = new SweepInvestmentResultEvent(
                null, SweepEventType.SWEEP_INVESTMENT_COMPLETED, "CORR",
                "SWEEP:POINT_LEDGER:1", 2L, 1L, 1L,
                userUuid, cardUserUuid, "2026-05", 1000L, 1000L, 100L, null, null
        );

        assertThrows(BusinessException.class, () -> service.validate(event));
    }

    @Test
    @DisplayName("sweepExecutionId가 없는 결과 이벤트는 검증에 실패한다")
    void validateInvalidEventWithoutSweepExecutionId() {
        SweepInvestmentResultEvent event = new SweepInvestmentResultEvent(
                "INVEST-SWEEP-TEST-1", SweepEventType.SWEEP_INVESTMENT_COMPLETED, "CORR",
                "SWEEP:POINT_LEDGER:1", 2L, null, 1L,
                userUuid, cardUserUuid, "2026-05", 1000L, 1000L, 100L, null, null
        );

        assertThrows(BusinessException.class, () -> service.validate(event));
    }

    private SweepInvestmentResultEvent completedEvent() {
        return new SweepInvestmentResultEvent(
                "INVEST-SWEEP-TEST-1",
                SweepEventType.SWEEP_INVESTMENT_COMPLETED,
                "CORR-SWEEP-TEST-1",
                "SWEEP:POINT_LEDGER:1",
                2L,
                1L,
                1L,
                userUuid,
                cardUserUuid,
                "2026-05",
                1000L,
                1000L,
                100L,
                null,
                null
        );
    }

    private SweepInvestmentResultEvent failedEvent() {
        return new SweepInvestmentResultEvent(
                "INVEST-SWEEP-TEST-1",
                SweepEventType.SWEEP_INVESTMENT_FAILED,
                "CORR-SWEEP-TEST-1",
                "SWEEP:POINT_LEDGER:1",
                2L,
                1L,
                1L,
                userUuid,
                cardUserUuid,
                "2026-05",
                1000L,
                1000L,
                100L,
                "INVEST_FAILED",
                "투자 실패"
        );
    }
}
