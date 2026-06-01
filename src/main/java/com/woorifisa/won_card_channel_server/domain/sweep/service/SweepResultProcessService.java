package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.woorifisa.won_card_channel_server.domain.sweep.dto.event.SweepInvestmentResultEvent;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.request.CardCoreSweepResultRequest;
import com.woorifisa.won_card_channel_server.domain.sweep.exception.code.SweepErrorCode;
import com.woorifisa.won_card_channel_server.domain.sweep.external.CardCoreRewardSweepApi;
import com.woorifisa.won_card_channel_server.domain.sweep.model.CardChnSweepRequest;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.CardChnSweepRequestRepository;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SweepResultProcessService {

    private final CardCoreRewardSweepApi cardCoreRewardSweepApi;
    private final CardChnSweepRequestRepository sweepRequestRepository;

    // 복구 불가능한 메세지 필터링
    public void validate(SweepInvestmentResultEvent event) {
        if (event == null
                || event.eventId() == null || event.eventId().isBlank()
                || event.eventType() == null
                || event.correlationId() == null || event.correlationId().isBlank()
                || event.idempotencyKey() == null || event.idempotencyKey().isBlank()
                || event.sweepRequestId() == null
                || event.pointLedgerId() == null
                || event.cardUserUuid() == null
                || (!event.completed() && !event.failed())) {
            throw new BusinessException(SweepErrorCode.SWEEP_INVALID_REQUEST);
        }
    }

    @Transactional
    public void process(Long inboxEventId, SweepInvestmentResultEvent event) {
        applyResultToCardCore(event);

        CardChnSweepRequest sweepRequest = sweepRequestRepository.findByIdempotencyKey(event.idempotencyKey())
                .orElseThrow(() -> new BusinessException(SweepErrorCode.SWEEP_REWARD_LEDGER_NOT_FOUND));

        if (event.completed()) {
            sweepRequest.markSucceeded();
            return;
        }

        sweepRequest.markFailed(event.failureMessage());
    }

    private void applyResultToCardCore(SweepInvestmentResultEvent event) {
        CardCoreSweepResultRequest request = new CardCoreSweepResultRequest(
                event.sweepRequestId(),
                event.sweepExecutionId(),
                event.correlationId(),
                event.idempotencyKey(),
                event.completed() ? "COMPLETED" : "FAILED"
        );

        try {
            ApiResponse<?> response = cardCoreRewardSweepApi.applySweepResult(
                    event.cardUserUuid(),
                    event.pointLedgerId(),
                    request
            );

            if (response == null || response.data() == null) {
                throw new BusinessException(
                        SweepErrorCode.SWEEP_CORE_RESPONSE_INVALID,
                        "카드 계정계 스윕 결과 반영 응답이 비어 있습니다."
                );
            }
        } catch (BusinessException e) {
            throw e;
        } catch (FeignException e) {
            throw new BusinessException(
                    SweepErrorCode.SWEEP_CORE_UNAVAILABLE,
                    "카드 계정계 호출 실패. status=" + e.status() + ", message=" + e.getMessage(),
                    e
            );
        }
    }

}
