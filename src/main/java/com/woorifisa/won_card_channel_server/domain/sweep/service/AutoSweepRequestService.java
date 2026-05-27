package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.command.AutoSweepTarget;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.event.SweepRequestedEvent;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.request.InternalSweepRequestCreateRequest;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.response.CardCoreSweepRequestResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.response.SweepRequestCreateResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.external.CardCoreRewardSweepApi;
import com.woorifisa.won_card_channel_server.domain.sweep.exception.code.SweepErrorCode;
import com.woorifisa.won_card_channel_server.domain.sweep.model.CardChnSweepOutbox;
import com.woorifisa.won_card_channel_server.domain.sweep.model.CardChnSweepRequest;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepEventType;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.CardChnSweepOutboxRepository;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.CardChnSweepRequestRepository;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AutoSweepRequestService {

    private static final String IDEMPOTENCY_KEY_PREFIX = "SWEEP:POINT_LEDGER:";
    private static final String EVENT_ID_PREFIX = "CARD-SWEEP-";

    private final CardCoreRewardSweepApi cardCoreRewardSweepApi;
    private final CardChnSweepRequestRepository cardChnSweepRequestRepository;
    private final CardChnSweepOutboxRepository cardChnSweepOutboxRepository;
    private final ObjectMapper objectMapper;

    private SweepRequestCreateResponse createSweepRequestForApi(AutoSweepTarget target) {

        validateTarget(target);

        String idempotencyKey = createIdempotencyKey(target.pointLedgerId());

        validateNotDuplicated(target.pointLedgerId(), idempotencyKey);

        String correlationId = UUID.randomUUID().toString();
        String eventId = EVENT_ID_PREFIX + UUID.randomUUID();

        try {
            // 스윕 요청 발행
            CardChnSweepRequest request = CardChnSweepRequest.createPendingPublish(target, correlationId, idempotencyKey);

            // 스윕 요청 DB 저장
            CardChnSweepRequest savedSweepRequest = cardChnSweepRequestRepository.save(request);

            // 저장된 스윕 요청 기반으로 카드 -> 증권으로 보낼 이벤트 객체 생성
            SweepRequestedEvent event = SweepRequestedEvent.from(savedSweepRequest, eventId);

            // SweepRequestedEvent 객체 JSON 문자열로 바꿈
            String payload = objectMapper.writeValueAsString(event);

            // outbox에 pending 상태로 저장
            CardChnSweepOutbox outbox = CardChnSweepOutbox.pending(
                    savedSweepRequest.getSweepRequestId(), eventId, SweepEventType.SWEEP_REQUESTED
                    , payload, correlationId, idempotencyKey);

            cardChnSweepOutboxRepository.save(outbox);

            return SweepRequestCreateResponse.from(savedSweepRequest);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(SweepErrorCode.SWEEP_ALREADY_REQUESTED, e);
        } catch (JsonProcessingException e) {
            throw new BusinessException(SweepErrorCode.SWEEP_OUTBOX_CREATE_FAILED, e);
        }

    }

    @Transactional
    public SweepRequestCreateResponse createSweepRequest(InternalSweepRequestCreateRequest request) {
        validateInternalRequest(request);

        String idempotencyKey = createIdempotencyKey(request.pointLedgerId());

        validateNotDuplicated(request.pointLedgerId(), idempotencyKey);

        CardCoreSweepRequestResponse coreResponse = requestSweepFromCore(
                request.cardUserUuid(),
                request.pointLedgerId()
        );

        AutoSweepTarget target = AutoSweepTarget.of(request, coreResponse);
        validateTarget(target);

        return createSweepRequestAndOutbox(target, idempotencyKey);
    }

    private CardCoreSweepRequestResponse requestSweepFromCore(UUID cardUserUuid, Long pointLedgerId) {
        try {
            ApiResponse<CardCoreSweepRequestResponse> coreResponse =
                    cardCoreRewardSweepApi.requestSweep(cardUserUuid, pointLedgerId);

            if (coreResponse == null || coreResponse.data() == null) {
                throw new BusinessException(SweepErrorCode.SWEEP_CORE_RESPONSE_INVALID);
            }

            return coreResponse.data();
        } catch (BusinessException e) {
            throw e;
        } catch (FeignException.Conflict e) {
            throw new BusinessException(SweepErrorCode.SWEEP_ALREADY_REQUESTED, e);
        } catch (FeignException.NotFound e) {
            throw new BusinessException(SweepErrorCode.SWEEP_REWARD_LEDGER_NOT_FOUND, e);
        } catch (FeignException.Forbidden e) {
            throw new BusinessException(SweepErrorCode.SWEEP_REWARD_LEDGER_FORBIDDEN, e);
        } catch (FeignException.UnprocessableEntity e) {
            throw new BusinessException(SweepErrorCode.SWEEP_REWARD_LEDGER_NOT_ELIGIBLE, e);
        } catch (FeignException e) {
            throw new BusinessException(SweepErrorCode.SWEEP_CORE_UNAVAILABLE, e);
        }
    }

    private SweepRequestCreateResponse createSweepRequestAndOutbox(
            AutoSweepTarget target,
            String idempotencyKey
    ) {
        String correlationId = UUID.randomUUID().toString();
        String eventId = EVENT_ID_PREFIX + UUID.randomUUID();

        try {
            CardChnSweepRequest sweepRequest = CardChnSweepRequest.createPendingPublish(
                    target,
                    correlationId,
                    idempotencyKey
            );

            CardChnSweepRequest savedSweepRequest = cardChnSweepRequestRepository.save(sweepRequest);

            SweepRequestedEvent event = SweepRequestedEvent.from(savedSweepRequest, eventId);
            String payload = objectMapper.writeValueAsString(event);

            CardChnSweepOutbox outbox = CardChnSweepOutbox.pending(
                    savedSweepRequest.getSweepRequestId(),
                    eventId,
                    SweepEventType.SWEEP_REQUESTED,
                    payload,
                    correlationId,
                    idempotencyKey
            );

            cardChnSweepOutboxRepository.save(outbox);

            return SweepRequestCreateResponse.from(savedSweepRequest);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateAfterPreCheck(target.pointLedgerId(), idempotencyKey)) {
                throw new BusinessException(SweepErrorCode.SWEEP_ALREADY_REQUESTED, e);
            }

            throw new BusinessException(SweepErrorCode.SWEEP_REQUEST_SAVE_FAILED, e);
        } catch (JsonProcessingException e) {
            throw new BusinessException(SweepErrorCode.SWEEP_OUTBOX_CREATE_FAILED, e);
        }
    }

    private boolean isDuplicateAfterPreCheck(Long pointLedgerId, String idempotencyKey) {
        return cardChnSweepRequestRepository.existsByPointLedgerId(pointLedgerId)
                || cardChnSweepRequestRepository.existsByIdempotencyKey(idempotencyKey);
    }

    private void validateTarget(AutoSweepTarget target) {
        if (target == null
                || target.userUuid() == null
                || target.cardUserUuid() == null
                || target.investUserUuid() == null
                || target.investAccountUuid() == null
                || target.performanceId() == null
                || target.pointLedgerId() == null
                || target.baseMonth() == null || target.baseMonth().isBlank()
                || target.pointAmount() == null
                || target.krwAmount() == null
                || target.etfId() == null
                || target.ticker() == null || target.ticker().isBlank()) {
            throw new BusinessException(SweepErrorCode.SWEEP_INVALID_REQUEST);
        }

        if (target.pointAmount() <= 0 || target.krwAmount() <= 0) {
            throw new BusinessException(SweepErrorCode.SWEEP_INVALID_REQUEST);
        }
    }

    private void validateInternalRequest(InternalSweepRequestCreateRequest request) {
        if (request == null
                || request.userUuid() == null
                || request.cardUserUuid() == null
                || request.investUserUuid() == null
                || request.investAccountUuid() == null
                || request.pointLedgerId() == null
                || request.etfId() == null
                || request.ticker() == null
                || request.ticker().isBlank()) {
            throw new BusinessException(SweepErrorCode.SWEEP_INVALID_REQUEST);
        }
    }

    // 중복 확인
    private void validateNotDuplicated(Long pointLedgerId, String idempotencyKey) {
        if (cardChnSweepRequestRepository.existsByPointLedgerId(pointLedgerId) ||
                cardChnSweepRequestRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new BusinessException(SweepErrorCode.SWEEP_ALREADY_REQUESTED);
        }
    }

    private String createIdempotencyKey(Long pointLedgerId) {
        return IDEMPOTENCY_KEY_PREFIX + pointLedgerId;
    }

}
