package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.woorifisa.won_card_channel_server.domain.autoinvest.service.AutoInvestSelectionPromotionServiceImpl;
import com.woorifisa.won_card_channel_server.domain.card.model.CardChnCardSummary;
import com.woorifisa.won_card_channel_server.domain.card.repository.CardChnCardSummaryRepository;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.command.AutoSweepCreateCommand;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.response.AutoSweepBatchResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.exception.code.SweepErrorCode;
import com.woorifisa.won_card_channel_server.domain.sweep.external.CardCoreRewardSweepApi;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepCandidateResponse;
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
    private AutoInvestSelectionPromotionServiceImpl autoInvestSelectionPromotionService;
    private AutoSweepBatchService autoSweepBatchService;

    private final UUID userUuid = UUID.fromString("a5324ba5-0ee3-44c6-b3d5-a951f9e94df5");
    private final UUID cardUserUuid = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        cardCoreRewardSweepApi = mock(CardCoreRewardSweepApi.class);
        cardSummaryRepository = mock(CardChnCardSummaryRepository.class);
        autoSweepRequestService = mock(AutoSweepRequestService.class);
        autoInvestSelectionPromotionService = mock(AutoInvestSelectionPromotionServiceImpl.class);

        autoSweepBatchService = new AutoSweepBatchService(
                cardCoreRewardSweepApi,
                cardSummaryRepository,
                autoSweepRequestService,
                autoInvestSelectionPromotionService
        );
    }

    @Test
    @DisplayName("Core 후보와 카드 요약 ETF가 있으면 단건 스윕 요청을 생성한다")
    void requestMonthlyAutoSweepsSuccess() {
        // given
        when(cardCoreRewardSweepApi.getSweepCandidates("2026-05"))
                .thenReturn(ApiResponse.of(
                        SuccessStatus.OK,
                        createCandidateResponse(List.of(createCandidate(1L, cardUserUuid, 12450L)))
                ));

        CardChnCardSummary cardSummary = mock(CardChnCardSummary.class);
        when(cardSummary.getUserUuid()).thenReturn(userUuid);
        when(cardSummary.getCardUserUuid()).thenReturn(cardUserUuid);
        when(cardSummary.getSelectedEtfId()).thenReturn(100L);

        when(cardSummaryRepository.findByCardUserUuid(cardUserUuid))
                .thenReturn(Optional.of(cardSummary));

        ArgumentCaptor<AutoSweepCreateCommand> commandCaptor =
                ArgumentCaptor.forClass(AutoSweepCreateCommand.class);

        // when
        AutoSweepBatchResponse response =
                autoSweepBatchService.requestMonthlyAutoSweeps("2026-05");

        // then
        assertThat(response.baseMonth()).isEqualTo("2026-05");
        assertThat(response.candidateCount()).isEqualTo(1);
        assertThat(response.requestedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(0);
        assertThat(response.failedCount()).isEqualTo(0);

        verify(autoSweepRequestService).createSweepRequest(commandCaptor.capture());

        AutoSweepCreateCommand command = commandCaptor.getValue();
        assertThat(command.userUuid()).isEqualTo(userUuid);
        assertThat(command.cardUserUuid()).isEqualTo(cardUserUuid);
        assertThat(command.pointLedgerId()).isEqualTo(1L);
        assertThat(command.etfId()).isEqualTo(100L);
        verify(autoInvestSelectionPromotionService).promoteEffectivePendingSelections(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("배치 시작 시 자동투자 예약 ETF 승격을 먼저 수행한 뒤 Core 후보를 조회한다")
    void requestMonthlyAutoSweepsPromotesPendingSelectionBeforeLoadingCandidates() {
        // given
        when(autoInvestSelectionPromotionService.promoteEffectivePendingSelections(any(LocalDateTime.class)))
                .thenReturn(2);
        when(cardCoreRewardSweepApi.getSweepCandidates("2026-05"))
                .thenReturn(ApiResponse.of(
                        SuccessStatus.OK,
                        createCandidateResponse(List.of())
                ));

        // when
        AutoSweepBatchResponse response =
                autoSweepBatchService.requestMonthlyAutoSweeps("2026-05");

        // then
        assertThat(response.candidateCount()).isEqualTo(0);
        assertThat(response.requestedCount()).isEqualTo(0);
        assertThat(response.skippedCount()).isEqualTo(0);
        assertThat(response.failedCount()).isEqualTo(0);

        var inOrder = inOrder(autoInvestSelectionPromotionService, cardCoreRewardSweepApi);
        inOrder.verify(autoInvestSelectionPromotionService)
                .promoteEffectivePendingSelections(any(LocalDateTime.class));
        inOrder.verify(cardCoreRewardSweepApi).getSweepCandidates("2026-05");
    }

    @Test
    @DisplayName("카드 요약이 없으면 후보를 skip한다")
    void requestMonthlyAutoSweepsSkipWhenCardSummaryNotFound() {
        // given
        when(cardCoreRewardSweepApi.getSweepCandidates("2026-05"))
                .thenReturn(ApiResponse.of(
                        SuccessStatus.OK,
                        createCandidateResponse(List.of(createCandidate(1L, cardUserUuid, 12450L)))
                ));

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

        verify(autoSweepRequestService, never()).createSweepRequest(any());
    }

    @Test
    @DisplayName("카드 요약에 ETF 설정이 없으면 후보를 skip한다")
    void requestMonthlyAutoSweepsSkipWhenEtfIdMissing() {
        // given
        when(cardCoreRewardSweepApi.getSweepCandidates("2026-05"))
                .thenReturn(ApiResponse.of(
                        SuccessStatus.OK,
                        createCandidateResponse(List.of(createCandidate(1L, cardUserUuid, 12450L)))
                ));

        CardChnCardSummary cardSummary = mock(CardChnCardSummary.class);
        when(cardSummary.getUserUuid()).thenReturn(userUuid);
        when(cardSummary.getSelectedEtfId()).thenReturn(null);

        when(cardSummaryRepository.findByCardUserUuid(cardUserUuid))
                .thenReturn(Optional.of(cardSummary));

        // when
        AutoSweepBatchResponse response =
                autoSweepBatchService.requestMonthlyAutoSweeps("2026-05");

        // then
        assertThat(response.candidateCount()).isEqualTo(1);
        assertThat(response.requestedCount()).isEqualTo(0);
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.failedCount()).isEqualTo(0);

        verify(autoSweepRequestService, never()).createSweepRequest(any());
    }

    @Test
    @DisplayName("이미 요청된 원장은 skip한다")
    void requestMonthlyAutoSweepsSkipWhenAlreadyRequested() {
        // given
        when(cardCoreRewardSweepApi.getSweepCandidates("2026-05"))
                .thenReturn(ApiResponse.of(
                        SuccessStatus.OK,
                        createCandidateResponse(List.of(createCandidate(1L, cardUserUuid, 12450L)))
                ));

        CardChnCardSummary cardSummary = mock(CardChnCardSummary.class);
        when(cardSummary.getUserUuid()).thenReturn(userUuid);
        when(cardSummary.getSelectedEtfId()).thenReturn(100L);

        when(cardSummaryRepository.findByCardUserUuid(cardUserUuid))
                .thenReturn(Optional.of(cardSummary));

        doThrow(new BusinessException(SweepErrorCode.SWEEP_ALREADY_REQUESTED))
                .when(autoSweepRequestService)
                .createSweepRequest(any(AutoSweepCreateCommand.class));

        // when
        AutoSweepBatchResponse response =
                autoSweepBatchService.requestMonthlyAutoSweeps("2026-05");

        // then
        assertThat(response.candidateCount()).isEqualTo(1);
        assertThat(response.requestedCount()).isEqualTo(0);
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.failedCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("단건 요청 생성 중 일반 비즈니스 예외가 발생하면 failedCount가 증가한다")
    void requestMonthlyAutoSweepsFailedWhenCreateRequestFails() {
        // given
        when(cardCoreRewardSweepApi.getSweepCandidates("2026-05"))
                .thenReturn(ApiResponse.of(
                        SuccessStatus.OK,
                        createCandidateResponse(List.of(createCandidate(1L, cardUserUuid, 12450L)))
                ));

        CardChnCardSummary cardSummary = mock(CardChnCardSummary.class);
        when(cardSummary.getUserUuid()).thenReturn(userUuid);
        when(cardSummary.getSelectedEtfId()).thenReturn(100L);

        when(cardSummaryRepository.findByCardUserUuid(cardUserUuid))
                .thenReturn(Optional.of(cardSummary));

        doThrow(new BusinessException(SweepErrorCode.SWEEP_CORE_UNAVAILABLE))
                .when(autoSweepRequestService)
                .createSweepRequest(any(AutoSweepCreateCommand.class));

        // when
        AutoSweepBatchResponse response =
                autoSweepBatchService.requestMonthlyAutoSweeps("2026-05");

        // then
        assertThat(response.candidateCount()).isEqualTo(1);
        assertThat(response.requestedCount()).isEqualTo(0);
        assertThat(response.skippedCount()).isEqualTo(0);
        assertThat(response.failedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Core 후보 응답 data가 null이면 예외가 발생한다")
    void requestMonthlyAutoSweepsCoreResponseDataNull() {
        // given
        when(cardCoreRewardSweepApi.getSweepCandidates("2026-05"))
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
        verify(cardCoreRewardSweepApi, never()).getSweepCandidates(any());
    }

    private CardCoreSweepCandidateResponse createCandidateResponse(
            List<CardCoreSweepCandidateResponse.CardCoreSweepCandidateItem> candidates
    ) {
        return new CardCoreSweepCandidateResponse("2026-05", candidates);
    }

    private CardCoreSweepCandidateResponse.CardCoreSweepCandidateItem createCandidate(
            Long pointLedgerId,
            UUID cardUserUuid,
            Long amount
    ) {
        return new CardCoreSweepCandidateResponse.CardCoreSweepCandidateItem(
                pointLedgerId,
                cardUserUuid,
                10L,
                "2026-05",
                amount,
                amount
        );
    }
}
