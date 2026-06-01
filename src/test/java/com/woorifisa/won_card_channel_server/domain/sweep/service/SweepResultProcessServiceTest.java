package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.woorifisa.won_card_channel_server.domain.sweep.dto.command.AutoSweepTarget;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.event.SweepInvestmentResultEvent;
import com.woorifisa.won_card_channel_server.domain.sweep.external.CardCoreRewardSweepApi;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepResultRequest;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepResultResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.model.CardChnSweepRequest;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepEventType;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepRequestStatus;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.CardChnSweepRequestRepository;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SweepResultProcessServiceTest {

    private CardCoreRewardSweepApi cardCoreRewardSweepApi;
    private CardChnSweepRequestRepository sweepRequestRepository;
    private SweepResultProcessService service;

    private final UUID userUuid = UUID.fromString("a5324ba5-0ee3-44c6-b3d5-a951f9e94df5");
    private final UUID cardUserUuid = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        cardCoreRewardSweepApi = mock(CardCoreRewardSweepApi.class);
        sweepRequestRepository = mock(CardChnSweepRequestRepository.class);
        service = new SweepResultProcessService(cardCoreRewardSweepApi, sweepRequestRepository);
    }

    @Test
    @DisplayName("투자 완료 결과를 Card Core에 반영하고 채널 요청 상태를 SUCCEEDED로 변경한다")
    void processCompleted() {
        SweepInvestmentResultEvent event = completedEvent();
        CardChnSweepRequest sweepRequest = sweepRequest();

        when(cardCoreRewardSweepApi.applySweepResult(any(), any(), any()))
                .thenReturn(ApiResponse.of(SuccessStatus.OK, new CardCoreSweepResultResponse(1L, "COMPLETED")));
        when(sweepRequestRepository.findByIdempotencyKey("SWEEP:POINT_LEDGER:1"))
                .thenReturn(Optional.of(sweepRequest));

        service.process(1L, event);

        assertThat(sweepRequest.getRequestStatus()).isEqualTo(SweepRequestStatus.SUCCEEDED);
        assertThat(sweepRequest.getCompletedAt()).isNotNull();
        assertThat(sweepRequest.getFailReason()).isNull();

        ArgumentCaptor<CardCoreSweepResultRequest> captor =
                ArgumentCaptor.forClass(CardCoreSweepResultRequest.class);

        verify(cardCoreRewardSweepApi).applySweepResult(eq(cardUserUuid), eq(1L), captor.capture());
        assertThat(captor.getValue().resultStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("투자 실패 결과를 Card Core에 반영하고 채널 요청 상태를 FAILED로 변경한다")
    void processFailed() {
        SweepInvestmentResultEvent event = failedEvent();
        CardChnSweepRequest sweepRequest = sweepRequest();

        when(cardCoreRewardSweepApi.applySweepResult(any(), any(), any()))
                .thenReturn(ApiResponse.of(SuccessStatus.OK, new CardCoreSweepResultResponse(1L, "FAILED")));
        when(sweepRequestRepository.findByIdempotencyKey("SWEEP:POINT_LEDGER:1"))
                .thenReturn(Optional.of(sweepRequest));

        service.process(1L, event);

        assertThat(sweepRequest.getRequestStatus()).isEqualTo(SweepRequestStatus.FAILED);
        assertThat(sweepRequest.getFailReason()).isEqualTo("투자 실패");
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

    private CardChnSweepRequest sweepRequest() {
        AutoSweepTarget target = new AutoSweepTarget(
                userUuid,
                cardUserUuid,
                10L,
                1L,
                "2026-05",
                1000L,
                1000L,
                100L
        );

        return CardChnSweepRequest.createPendingPublish(
                target,
                "CORR-SWEEP-TEST-1",
                "SWEEP:POINT_LEDGER:1"
        );
    }
}
