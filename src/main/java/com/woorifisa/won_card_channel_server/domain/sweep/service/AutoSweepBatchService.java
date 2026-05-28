package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.woorifisa.won_card_channel_server.domain.card.model.CardChnCardSummary;
import com.woorifisa.won_card_channel_server.domain.card.repository.CardChnCardSummaryRepository;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.command.AutoSweepCreateCommand;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.response.AutoSweepBatchResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.exception.code.SweepErrorCode;
import com.woorifisa.won_card_channel_server.domain.sweep.external.CardCoreRewardSweepApi;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepCandidateResponse;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AutoSweepBatchService {

    private static final Pattern BASE_MONTH_PATTERN = Pattern.compile("\\d{4}-\\d{2}");

    private final CardCoreRewardSweepApi cardCoreRewardSweepApi;
    private final CardChnCardSummaryRepository cardSummaryRepository;
    private final AutoSweepRequestService autoSweepRequestService;

    public AutoSweepBatchResponse requestMonthlyAutoSweeps(String baseMonth) {
        validateBaseMonth(baseMonth);

        // Core 후보 조회 Feign 호출
        ApiResponse<CardCoreSweepCandidateResponse> apiResponse;

        try {
            apiResponse = cardCoreRewardSweepApi.getSweepCandidates(baseMonth);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(SweepErrorCode.SWEEP_CORE_UNAVAILABLE, e);
        }

        if (apiResponse == null || apiResponse.data() == null || apiResponse.data().candidates() == null) {
            throw new BusinessException(SweepErrorCode.SWEEP_CORE_RESPONSE_INVALID);
        }

        List<CardCoreSweepCandidateResponse.CardCoreSweepCandidateItem> candidates =
                apiResponse.data().candidates();

        int requestedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (CardCoreSweepCandidateResponse.CardCoreSweepCandidateItem candidate : candidates) {
            try {
                if (candidate == null || candidate.cardUserUuid() == null || candidate.pointLedgerId() == null) {
                    skippedCount++;
                    continue;
                }

                // 후보별 cardUserUuid로 card_chn_card_summary 조회
                CardChnCardSummary cardSummary = cardSummaryRepository
                        .findByCardUserUuid(candidate.cardUserUuid())
                        .orElse(null);

                if (cardSummary == null
                        || cardSummary.getUserUuid() == null
                        || cardSummary.getSelectedEtfId() == null) {
                    skippedCount++;
                    continue;
                }

                // InternalSweepRequestCreateRequest 생성
                AutoSweepCreateCommand request =
                        new AutoSweepCreateCommand(
                                cardSummary.getUserUuid(),
                                candidate.cardUserUuid(),
                                candidate.pointLedgerId(),
                                cardSummary.getSelectedEtfId());

                autoSweepRequestService.createSweepRequest(request);
                requestedCount++;
            } catch (BusinessException e) {
                if (e.getErrorCode() == SweepErrorCode.SWEEP_ALREADY_REQUESTED) {
                    skippedCount++;
                } else {
                    failedCount++;
                }
            } catch (Exception e) {
                failedCount++;
            }
        }

        return new AutoSweepBatchResponse(
                baseMonth,
                candidates.size(),
                requestedCount,
                skippedCount,
                failedCount
        );
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
}
