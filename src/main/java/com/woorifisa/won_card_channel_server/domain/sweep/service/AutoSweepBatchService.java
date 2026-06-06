package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.woorifisa.won_card_channel_server.domain.autoinvest.service.AutoInvestSelectionPromotionService;
import com.woorifisa.won_card_channel_server.domain.card.model.CardChnCardSummary;
import com.woorifisa.won_card_channel_server.domain.card.repository.CardChnCardSummaryRepository;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.command.ReservedSweepCreateCommand;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.response.AutoSweepBatchResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.result.ReservedItemProcessResult;
import com.woorifisa.won_card_channel_server.domain.sweep.exception.code.SweepErrorCode;
import com.woorifisa.won_card_channel_server.domain.sweep.external.CardCoreRewardSweepApi;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepBatchStartRequest;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepBatchStartResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepReservationResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepReservedItemResponse;
import com.woorifisa.won_card_channel_server.global.config.SweepBatchProperties;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoSweepBatchService {

    private static final int MONTHLY_BATCH_DAY = 16;
    private static final int MONTHLY_BATCH_HOUR = 0;
    private static final int MONTHLY_BATCH_MINUTE = 30;
    private static final Pattern BASE_MONTH_PATTERN = Pattern.compile("\\d{4}-\\d{2}");

    private final CardCoreRewardSweepApi cardCoreRewardSweepApi;
    private final CardChnCardSummaryRepository cardSummaryRepository;
    private final AutoSweepRequestService autoSweepRequestService;
    private final AutoInvestSelectionPromotionService autoInvestSelectionPromotionService;
    private final SweepBatchProperties sweepBatchProperties;

    public AutoSweepBatchResponse requestMonthlyAutoSweeps(String baseMonth) {
        validateBaseMonth(baseMonth);

        LocalDateTime batchStartedAt = calculateMonthlyBatchStartedAt(baseMonth);
        int promotedCount = autoInvestSelectionPromotionService.promoteEffectivePendingSelections(batchStartedAt);
        log.info("자동투자 예약 ETF 승격 완료. baseMonth={}, promotedCount={}", baseMonth, promotedCount);

        int reservationSize = sweepBatchProperties.reservationSize();
        CardCoreSweepBatchStartResponse batch = startSweepBatch(baseMonth, reservationSize);

        int candidateCount = 0;
        int requestedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        Set<Long> reservedPointLedgerIds = new HashSet<>();

        while (true) {
            CardCoreSweepReservationResponse reservation =
                    reserveSweepBatch(batch.batchExecutionId(), reservationSize);

            List<CardCoreSweepReservedItemResponse> reservedItems = reservation.reservedItems();

            if (reservedItems.isEmpty()) {
                break;
            }

            validateReservationItems(reservedItems, baseMonth, reservedPointLedgerIds);

            candidateCount += reservedItems.size();

            for (CardCoreSweepReservedItemResponse reservedItem : reservedItems) {
                ReservedItemProcessResult result = processReservedItem(reservedItem);

                switch (result) {
                    case REQUESTED -> requestedCount++;
                    case SKIPPED -> skippedCount++;
                    case FAILED -> failedCount++;
                }
            }
        }

        return new AutoSweepBatchResponse(
                baseMonth,
                candidateCount,
                requestedCount,
                skippedCount,
                failedCount
        );
    }

    private ReservedItemProcessResult processReservedItem(CardCoreSweepReservedItemResponse reservedItem) {
        try {
            validateReservedItem(reservedItem);

            CardChnCardSummary cardSummary = cardSummaryRepository
                    .findByCardUserUuid(reservedItem.cardUserUuid())
                    .orElse(null);

            if (cardSummary == null
                    || cardSummary.getUserUuid() == null
                    || cardSummary.getSelectedEtfId() == null) {
                compensateSweepRequest(reservedItem.cardUserUuid(), reservedItem.pointLedgerId());
                log.warn(
                        "자동 스윕 예약 항목을 skip했습니다. 카드 요약 매핑 또는 ETF 선택이 없습니다. pointLedgerId={}, cardUserUuid={}",
                        reservedItem.pointLedgerId(),
                        reservedItem.cardUserUuid()
                );
                return ReservedItemProcessResult.SKIPPED;
            }

            ReservedSweepCreateCommand command = new ReservedSweepCreateCommand(
                    cardSummary.getUserUuid(),
                    reservedItem.cardUserUuid(),
                    reservedItem.performanceId(),
                    reservedItem.pointLedgerId(),
                    reservedItem.baseMonth(),
                    reservedItem.pointAmount(),
                    reservedItem.krwAmount(),
                    cardSummary.getSelectedEtfId(),
                    reservedItem.eventId(),
                    reservedItem.correlationId(),
                    reservedItem.idempotencyKey(),
                    reservedItem.requestedAt()
            );

            autoSweepRequestService.createSweepRequestFromReservedItem(command);
            return ReservedItemProcessResult.REQUESTED;
        } catch (BusinessException e) {
            if (e.getErrorCode() == SweepErrorCode.SWEEP_ALREADY_REQUESTED) {
                log.info(
                        "자동 스윕 예약 항목을 skip했습니다. 이미 Channel 요청이 존재합니다. pointLedgerId={}, cardUserUuid={}",
                        pointLedgerIdOf(reservedItem),
                        cardUserUuidOf(reservedItem)
                );
                return ReservedItemProcessResult.SKIPPED;
            }

            compensateSweepRequest(cardUserUuidOf(reservedItem), pointLedgerIdOf(reservedItem));
            log.warn(
                    "자동 스윕 예약 항목 처리 중 비즈니스 예외가 발생했습니다. pointLedgerId={}, cardUserUuid={}, errorCode={}",
                    pointLedgerIdOf(reservedItem),
                    cardUserUuidOf(reservedItem),
                    e.getErrorCode().getCode(),
                    e
            );
            return ReservedItemProcessResult.FAILED;
        } catch (Exception e) {
            compensateSweepRequest(cardUserUuidOf(reservedItem), pointLedgerIdOf(reservedItem));
            log.warn(
                    "자동 스윕 예약 항목 처리 중 예외가 발생했습니다. pointLedgerId={}, cardUserUuid={}",
                    pointLedgerIdOf(reservedItem),
                    cardUserUuidOf(reservedItem),
                    e
            );
            return ReservedItemProcessResult.FAILED;
        }
    }

    private CardCoreSweepBatchStartResponse startSweepBatch(String baseMonth, int reservationSize) {
        ApiResponse<CardCoreSweepBatchStartResponse> apiResponse;

        try {
            apiResponse = cardCoreRewardSweepApi.startSweepBatch(
                    new CardCoreSweepBatchStartRequest(baseMonth, reservationSize)
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(SweepErrorCode.SWEEP_CORE_UNAVAILABLE, e);
        }

        if (apiResponse == null
                || apiResponse.data() == null
                || apiResponse.data().batchExecutionId() == null
                || apiResponse.data().baseMonth() == null
                || !baseMonth.equals(apiResponse.data().baseMonth())) {
            throw new BusinessException(SweepErrorCode.SWEEP_CORE_RESPONSE_INVALID);
        }

        return apiResponse.data();
    }

    private CardCoreSweepReservationResponse reserveSweepBatch(Long batchExecutionId, int reservationSize) {
        ApiResponse<CardCoreSweepReservationResponse> apiResponse;

        try {
            apiResponse = cardCoreRewardSweepApi.reserveSweepBatch(batchExecutionId, reservationSize);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(SweepErrorCode.SWEEP_CORE_UNAVAILABLE, e);
        }

        if (apiResponse == null
                || apiResponse.data() == null
                || apiResponse.data().batchExecutionId() == null
                || apiResponse.data().reservedItems() == null) {
            throw new BusinessException(SweepErrorCode.SWEEP_CORE_RESPONSE_INVALID);
        }

        if (!batchExecutionId.equals(apiResponse.data().batchExecutionId())) {
            throw new BusinessException(SweepErrorCode.SWEEP_CORE_RESPONSE_INVALID);
        }

        return apiResponse.data();
    }

    private void validateReservationItems(
            List<CardCoreSweepReservedItemResponse> reservedItems,
            String baseMonth,
            Set<Long> reservedPointLedgerIds
    ) {
        boolean hasNewReservedItem = false;

        for (CardCoreSweepReservedItemResponse reservedItem : reservedItems) {
            validateReservedItem(reservedItem);

            if (!baseMonth.equals(reservedItem.baseMonth())) {
                throw new BusinessException(SweepErrorCode.SWEEP_CORE_RESPONSE_INVALID);
            }

            if (reservedPointLedgerIds.add(reservedItem.pointLedgerId())) {
                hasNewReservedItem = true;
            }
        }

        if (!hasNewReservedItem) {
            throw new BusinessException(SweepErrorCode.SWEEP_CORE_RESPONSE_INVALID);
        }
    }

    private void validateReservedItem(CardCoreSweepReservedItemResponse reservedItem) {
        if (reservedItem == null
                || reservedItem.cardUserUuid() == null
                || reservedItem.performanceId() == null
                || reservedItem.pointLedgerId() == null
                || reservedItem.baseMonth() == null || reservedItem.baseMonth().isBlank()
                || reservedItem.pointAmount() == null
                || reservedItem.krwAmount() == null
                || reservedItem.eventId() == null || reservedItem.eventId().isBlank()
                || reservedItem.correlationId() == null || reservedItem.correlationId().isBlank()
                || reservedItem.idempotencyKey() == null || reservedItem.idempotencyKey().isBlank()
                || reservedItem.requestedAt() == null) {
            throw new BusinessException(SweepErrorCode.SWEEP_INVALID_REQUEST);
        }
    }

    private void compensateSweepRequest(UUID cardUserUuid, Long pointLedgerId) {
        if (cardUserUuid == null || pointLedgerId == null) {
            return;
        }

        try {
            cardCoreRewardSweepApi.cancelSweepRequest(cardUserUuid, pointLedgerId);
        } catch (Exception compensationException) {
            log.warn(
                    "Core 스윕 보상 호출에 실패했습니다. cardUserUuid={}, pointLedgerId={}",
                    cardUserUuid,
                    pointLedgerId,
                    compensationException
            );
        }
    }

    private UUID cardUserUuidOf(CardCoreSweepReservedItemResponse reservedItem) {
        return reservedItem == null ? null : reservedItem.cardUserUuid();
    }

    private Long pointLedgerIdOf(CardCoreSweepReservedItemResponse reservedItem) {
        return reservedItem == null ? null : reservedItem.pointLedgerId();
    }

    private void validateBaseMonth(String baseMonth) {
        if (baseMonth == null || !BASE_MONTH_PATTERN.matcher(baseMonth).matches()) {
            throw new BusinessException(SweepErrorCode.SWEEP_INVALID_REQUEST);
        }

        try {
            YearMonth.parse(baseMonth);
        } catch (DateTimeParseException e) {
            throw new BusinessException(SweepErrorCode.SWEEP_INVALID_REQUEST);
        }
    }

    private LocalDateTime calculateMonthlyBatchStartedAt(String baseMonth) {
        return YearMonth.parse(baseMonth)
                .atDay(MONTHLY_BATCH_DAY)
                .atTime(MONTHLY_BATCH_HOUR, MONTHLY_BATCH_MINUTE);
    }

}
