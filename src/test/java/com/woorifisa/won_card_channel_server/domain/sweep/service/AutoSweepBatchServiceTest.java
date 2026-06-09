package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.woorifisa.won_card_channel_server.domain.autoinvest.service.AutoInvestSelectionPromotionService;
import com.woorifisa.won_card_channel_server.domain.card.model.CardChnCardSummary;
import com.woorifisa.won_card_channel_server.domain.card.repository.CardChnCardSummaryRepository;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.command.ReservedSweepCreateCommand;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.response.AutoSweepBatchResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.exception.code.SweepErrorCode;
import com.woorifisa.won_card_channel_server.domain.sweep.external.CardCoreRewardSweepApi;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepBatchStartRequest;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepBatchStartResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepCancelResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepReservationResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepReservedItemResponse;
import com.woorifisa.won_card_channel_server.global.config.SweepBatchProperties;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AutoSweepBatchServiceTest {

    private CardCoreRewardSweepApi cardCoreRewardSweepApi;
    private CardChnCardSummaryRepository cardSummaryRepository;
    private AutoSweepRequestService autoSweepRequestService;
    private AutoInvestSelectionPromotionService autoInvestSelectionPromotionService;
    private AutoSweepBatchService autoSweepBatchService;

    private final UUID userUuid = UUID.fromString("a5324ba5-0ee3-44c6-b3d5-a951f9e94df5");
    private final UUID cardUserUuid = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final LocalDateTime requestedAt = LocalDateTime.of(2026, 5, 16, 0, 30);

    @BeforeEach
    void setUp() {
        cardCoreRewardSweepApi = mock(CardCoreRewardSweepApi.class);
        cardSummaryRepository = mock(CardChnCardSummaryRepository.class);
        autoSweepRequestService = mock(AutoSweepRequestService.class);
        autoInvestSelectionPromotionService = mock(AutoInvestSelectionPromotionService.class);

        autoSweepBatchService = new AutoSweepBatchService(
                cardCoreRewardSweepApi,
                cardSummaryRepository,
                autoSweepRequestService,
                autoInvestSelectionPromotionService,
                new SweepBatchProperties(500)
        );
    }

    @Test
    @DisplayName("Core reservation 항목에 userUuid와 ETF를 보강해 Channel 요청을 생성한다")
    void requestMonthlyAutoSweepsSuccess() {
        // given
        when(cardCoreRewardSweepApi.startSweepBatch(any(CardCoreSweepBatchStartRequest.class)))
                .thenReturn(ApiResponse.of(SuccessStatus.OK, createBatchStartResponse()));
        when(cardCoreRewardSweepApi.reserveSweepBatch(10L, 500))
                .thenReturn(ApiResponse.of(
                        SuccessStatus.OK,
                        createReservationResponse(List.of(createReservedItem(1L, cardUserUuid, 12450L)))
                ))
                .thenReturn(ApiResponse.of(
                        SuccessStatus.OK,
                        createReservationResponse(List.of())
                ));

        CardChnCardSummary cardSummary = mock(CardChnCardSummary.class);
        when(cardSummary.getUserUuid()).thenReturn(userUuid);
        when(cardSummary.getSelectedEtfId()).thenReturn(100L);

        when(cardSummaryRepository.findByCardUserUuid(cardUserUuid))
                .thenReturn(Optional.of(cardSummary));

        ArgumentCaptor<ReservedSweepCreateCommand> commandCaptor =
                ArgumentCaptor.forClass(ReservedSweepCreateCommand.class);

        // when
        AutoSweepBatchResponse response =
                autoSweepBatchService.requestMonthlyAutoSweeps("2026-05");

        // then
        assertThat(response.baseMonth()).isEqualTo("2026-05");
        assertThat(response.candidateCount()).isEqualTo(1);
        assertThat(response.requestedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(0);
        assertThat(response.failedCount()).isEqualTo(0);

        verify(autoSweepRequestService).createSweepRequestFromReservedItem(commandCaptor.capture());

        ReservedSweepCreateCommand command = commandCaptor.getValue();
        assertThat(command.userUuid()).isEqualTo(userUuid);
        assertThat(command.cardUserUuid()).isEqualTo(cardUserUuid);
        assertThat(command.pointLedgerId()).isEqualTo(1L);
        assertThat(command.etfId()).isEqualTo(100L);
        assertThat(command.eventId()).isEqualTo("CARD-SWEEP-1");
        assertThat(command.correlationId()).isEqualTo("CARD-SWEEP-CORR-1");
        assertThat(command.idempotencyKey()).isEqualTo("CARD_SWEEP:1:2026-05");
        assertThat(command.requestedAt()).isEqualTo(requestedAt);
        verify(autoInvestSelectionPromotionService).promoteEffectivePendingSelections(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("배치 시작 시 자동투자 예약 ETF 승격 후 Core batch start를 호출한다")
    void requestMonthlyAutoSweepsPromotesPendingSelectionBeforeStartingBatch() {
        // given
        when(autoInvestSelectionPromotionService.promoteEffectivePendingSelections(any(LocalDateTime.class)))
                .thenReturn(2);
        when(cardCoreRewardSweepApi.startSweepBatch(any(CardCoreSweepBatchStartRequest.class)))
                .thenReturn(ApiResponse.of(SuccessStatus.OK, createBatchStartResponse()));
        when(cardCoreRewardSweepApi.reserveSweepBatch(10L, 500))
                .thenReturn(ApiResponse.of(SuccessStatus.OK, createReservationResponse(List.of())));

        // when
        AutoSweepBatchResponse response =
                autoSweepBatchService.requestMonthlyAutoSweeps("2026-05");

        // then
        assertThat(response.candidateCount()).isEqualTo(0);
        assertThat(response.requestedCount()).isEqualTo(0);
        assertThat(response.skippedCount()).isEqualTo(0);
        assertThat(response.failedCount()).isEqualTo(0);

        var inOrder = inOrder(autoInvestSelectionPromotionService, cardCoreRewardSweepApi);
        ArgumentCaptor<LocalDateTime> batchStartedAtCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<CardCoreSweepBatchStartRequest> startRequestCaptor =
                ArgumentCaptor.forClass(CardCoreSweepBatchStartRequest.class);

        inOrder.verify(autoInvestSelectionPromotionService)
                .promoteEffectivePendingSelections(batchStartedAtCaptor.capture());
        inOrder.verify(cardCoreRewardSweepApi).startSweepBatch(startRequestCaptor.capture());

        assertThat(batchStartedAtCaptor.getValue())
                .isEqualTo(LocalDateTime.of(2026, 5, 16, 0, 30));
        assertThat(startRequestCaptor.getValue().baseMonth()).isEqualTo("2026-05");
        assertThat(startRequestCaptor.getValue().chunkSize()).isEqualTo(500);
    }

    @Test
    @DisplayName("카드 요약이 없으면 Core cancel을 호출하고 skip한다")
    void requestMonthlyAutoSweepsSkipWhenCardSummaryNotFound() {
        // given
        when(cardCoreRewardSweepApi.startSweepBatch(any(CardCoreSweepBatchStartRequest.class)))
                .thenReturn(ApiResponse.of(SuccessStatus.OK, createBatchStartResponse()));
        when(cardCoreRewardSweepApi.reserveSweepBatch(10L, 500))
                .thenReturn(ApiResponse.of(
                        SuccessStatus.OK,
                        createReservationResponse(List.of(createReservedItem(1L, cardUserUuid, 12450L)))
                ))
                .thenReturn(ApiResponse.of(SuccessStatus.OK, createReservationResponse(List.of())));
        when(cardCoreRewardSweepApi.cancelSweepRequest(cardUserUuid, 1L))
                .thenReturn(ApiResponse.of(SuccessStatus.OK, new CardCoreSweepCancelResponse(1L, "NONE")));
        when(cardSummaryRepository.findByCardUserUuid(cardUserUuid))
                .thenReturn(Optional.empty());

        // when
        AutoSweepBatchResponse response =
                autoSweepBatchService.requestMonthlyAutoSweeps("2026-05");

        // then
        assertThat(response.candidateCount()).isEqualTo(1);
        assertThat(response.requestedCount()).isEqualTo(0);
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.failedCount()).isEqualTo(0);

        verify(autoSweepRequestService, never()).createSweepRequestFromReservedItem(any());
        verify(cardCoreRewardSweepApi).cancelSweepRequest(cardUserUuid, 1L);
    }

    @Test
    @DisplayName("이미 Channel 요청이 있으면 Core cancel 없이 skip한다")
    void requestMonthlyAutoSweepsSkipWhenAlreadyRequested() {
        // given
        when(cardCoreRewardSweepApi.startSweepBatch(any(CardCoreSweepBatchStartRequest.class)))
                .thenReturn(ApiResponse.of(SuccessStatus.OK, createBatchStartResponse()));
        when(cardCoreRewardSweepApi.reserveSweepBatch(10L, 500))
                .thenReturn(ApiResponse.of(
                        SuccessStatus.OK,
                        createReservationResponse(List.of(createReservedItem(1L, cardUserUuid, 12450L)))
                ))
                .thenReturn(ApiResponse.of(SuccessStatus.OK, createReservationResponse(List.of())));

        CardChnCardSummary cardSummary = mock(CardChnCardSummary.class);
        when(cardSummary.getUserUuid()).thenReturn(userUuid);
        when(cardSummary.getSelectedEtfId()).thenReturn(100L);
        when(cardSummaryRepository.findByCardUserUuid(cardUserUuid))
                .thenReturn(Optional.of(cardSummary));

        doThrow(new BusinessException(SweepErrorCode.SWEEP_ALREADY_REQUESTED))
                .when(autoSweepRequestService)
                .createSweepRequestFromReservedItem(any(ReservedSweepCreateCommand.class));

        // when
        AutoSweepBatchResponse response =
                autoSweepBatchService.requestMonthlyAutoSweeps("2026-05");

        // then
        assertThat(response.candidateCount()).isEqualTo(1);
        assertThat(response.requestedCount()).isEqualTo(0);
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.failedCount()).isEqualTo(0);
        verify(cardCoreRewardSweepApi, never()).cancelSweepRequest(any(), any());
    }

    @Test
    @DisplayName("Core batch start 응답 data가 null이면 예외가 발생한다")
    void requestMonthlyAutoSweepsCoreResponseDataNull() {
        // given
        when(cardCoreRewardSweepApi.startSweepBatch(any(CardCoreSweepBatchStartRequest.class)))
                .thenReturn(ApiResponse.of(SuccessStatus.OK, null));

        // when & then
        assertThatThrownBy(() -> autoSweepBatchService.requestMonthlyAutoSweeps("2026-05"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", SweepErrorCode.SWEEP_CORE_RESPONSE_INVALID);
    }

    @Test
    @DisplayName("baseMonth 형식이 yyyy-MM이 아니면 예외가 발생한다")
    void requestMonthlyAutoSweepsInvalidBaseMonth() {
        assertThatThrownBy(() -> autoSweepBatchService.requestMonthlyAutoSweeps("202605"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", SweepErrorCode.SWEEP_INVALID_REQUEST);

        verify(autoInvestSelectionPromotionService, never()).promoteEffectivePendingSelections(any());
        verify(cardCoreRewardSweepApi, never()).startSweepBatch(any());
    }

    private CardCoreSweepBatchStartResponse createBatchStartResponse() {
        return new CardCoreSweepBatchStartResponse(10L, "2026-05", "RUNNING", 0L);
    }

    private CardCoreSweepReservationResponse createReservationResponse(
            List<CardCoreSweepReservedItemResponse> reservedItems
    ) {
        return new CardCoreSweepReservationResponse(
                10L,
                "2026-05",
                reservedItems.isEmpty() ? "COMPLETED" : "RUNNING",
                reservedItems.size(),
                reservedItems.isEmpty() ? 1L : reservedItems.get(reservedItems.size() - 1).pointLedgerId(),
                reservedItems
        );
    }

    private CardCoreSweepReservedItemResponse createReservedItem(
            Long pointLedgerId,
            UUID cardUserUuid,
            Long amount
    ) {
        return new CardCoreSweepReservedItemResponse(
                pointLedgerId,
                "SWEEP_REQUESTED",
                "CARD-SWEEP-" + pointLedgerId,
                "CARD-SWEEP-CORR-" + pointLedgerId,
                "CARD_SWEEP:%d:2026-05".formatted(pointLedgerId),
                cardUserUuid,
                10L,
                pointLedgerId,
                "2026-05",
                amount,
                amount,
                requestedAt
        );
    }
}
