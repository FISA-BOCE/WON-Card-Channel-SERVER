package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.woorifisa.won_card_channel_server.domain.sweep.dto.command.AutoSweepTarget;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.event.SweepInvestmentResultEvent;
import com.woorifisa.won_card_channel_server.domain.sweep.model.Sweep;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepEventType;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepProcessStatus;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.SweepRepository;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SweepStatusUpdateServiceTest {

    private SweepRepository sweepRequestRepository;
    private SweepStatusUpdateService service;

    private final UUID userUuid = UUID.fromString("a5324ba5-0ee3-44c6-b3d5-a951f9e94df5");
    private final UUID cardUserUuid = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        sweepRequestRepository = mock(SweepRepository.class);
        service = new SweepStatusUpdateService(sweepRequestRepository);
    }

    @Test
    @DisplayName("투자 완료 결과를 채널 요청 상태 SUCCEEDED로 변경한다")
    void updateCompleted() {
        SweepInvestmentResultEvent event = completedEvent();
        Sweep sweep = sweepRequest();

        when(sweepRequestRepository.findByIdempotencyKey("SWEEP:POINT_LEDGER:1"))
                .thenReturn(Optional.of(sweep));

        service.update(event);

        assertThat(sweep.getRequestStatus()).isEqualTo(SweepProcessStatus.SUCCEEDED);
        assertThat(sweep.getCompletedAt()).isNotNull();
        assertThat(sweep.getFailReason()).isNull();
    }

    @Test
    @DisplayName("투자 실패 결과를 채널 요청 상태 FAILED로 변경한다")
    void updateFailed() {
        SweepInvestmentResultEvent event = failedEvent();
        Sweep sweep = sweepRequest();

        when(sweepRequestRepository.findByIdempotencyKey("SWEEP:POINT_LEDGER:1"))
                .thenReturn(Optional.of(sweep));

        service.update(event);

        assertThat(sweep.getRequestStatus()).isEqualTo(SweepProcessStatus.FAILED);
        assertThat(sweep.getCompletedAt()).isNotNull();
        assertThat(sweep.getFailReason()).isEqualTo("투자 실패");
    }

    @Test
    @DisplayName("idempotencyKey에 해당하는 스윕 요청이 없으면 예외를 던진다")
    void updateNotFound() {
        SweepInvestmentResultEvent event = completedEvent();

        when(sweepRequestRepository.findByIdempotencyKey("SWEEP:POINT_LEDGER:1"))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.update(event));
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

    private Sweep sweepRequest() {
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

        return Sweep.createPendingPublish(
                target,
                "CORR-SWEEP-TEST-1",
                "SWEEP:POINT_LEDGER:1"
        );
    }
}
