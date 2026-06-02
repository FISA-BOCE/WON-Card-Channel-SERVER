package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.woorifisa.won_card_channel_server.domain.sweep.dto.event.SweepInvestmentResultEvent;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepResultRequest;
import com.woorifisa.won_card_channel_server.domain.sweep.exception.code.SweepErrorCode;
import com.woorifisa.won_card_channel_server.domain.sweep.external.CardCoreRewardSweepApi;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SweepResultProcessService {

    private final CardCoreRewardSweepApi cardCoreRewardSweepApi;
    private final SweepStatusUpdateService sweepStatusUpdateService;

    // 복구 불가능한 메세지 필터링
    public void validate(SweepInvestmentResultEvent event) {
        boolean completed = event != null && event.completed();
        boolean failed = event != null && event.failed();

        if (event == null
                || event.eventId() == null || event.eventId().isBlank()
                || event.eventType() == null
                || event.correlationId() == null || event.correlationId().isBlank()
                || event.idempotencyKey() == null || event.idempotencyKey().isBlank()
                || event.sweepRequestId() == null
                || event.sweepExecutionId() == null
                || event.pointLedgerId() == null
                || event.cardUserUuid() == null
                || completed == failed) {
            throw new BusinessException(SweepErrorCode.SWEEP_INVALID_REQUEST);
        }
    }

    public void process(SweepInvestmentResultEvent event) {
        sweepStatusUpdateService.update(event);
        applyResultToCardCore(event);
    }

    private void applyResultToCardCore(SweepInvestmentResultEvent event) {
        CardCoreSweepResultRequest request = CardCoreSweepResultRequest.from(event);

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
