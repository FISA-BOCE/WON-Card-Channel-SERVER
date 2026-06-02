package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.command.AutoSweepCreateCommand;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepRequestResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.response.SweepRequestCreateResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.exception.code.SweepErrorCode;
import com.woorifisa.won_card_channel_server.domain.sweep.external.CardCoreRewardSweepApi;
import com.woorifisa.won_card_channel_server.domain.sweep.model.SweepOutbox;
import com.woorifisa.won_card_channel_server.domain.sweep.model.Sweep;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.OutboxPublishStatus;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepEventType;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepProcessStatus;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.SweepOutboxRepository;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.SweepRepository;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AutoSweepServiceTest {

    private CardCoreRewardSweepApi cardCoreRewardSweepApi;
    private SweepRepository sweepRepository;
    private SweepOutboxRepository sweepOutboxRepository;
    private ObjectMapper objectMapper;
    private AutoSweepRequestService autoSweepRequestService;

    private final UUID userUuid = UUID.fromString("a5324ba5-0ee3-44c6-b3d5-a951f9e94df5");
    private final UUID cardUserUuid = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        cardCoreRewardSweepApi = mock(CardCoreRewardSweepApi.class);
        sweepRepository = mock(SweepRepository.class);
        sweepOutboxRepository = mock(SweepOutboxRepository.class);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        autoSweepRequestService = new AutoSweepRequestService(
                cardCoreRewardSweepApi,
                sweepRepository,
                sweepOutboxRepository,
                objectMapper
        );
    }

    @Test
    @DisplayName("정상 요청이면 Core 선점 후 sweep_request와 sweep_outbox를 저장한다")
    void createSweepRequestSuccess() {
        // given
        AutoSweepCreateCommand request = createSweepRequestRequest(1L);

        when(sweepRepository.existsByPointLedgerId(1L)).thenReturn(false);
        when(sweepRepository.existsByIdempotencyKey("SWEEP:POINT_LEDGER:1")).thenReturn(false);

        when(cardCoreRewardSweepApi.requestSweep(cardUserUuid, 1L))
                .thenReturn(ApiResponse.of(SuccessStatus.OK, createSweepRequestCoreResponse(1L, 12450L)));

        when(sweepRepository.save(any(Sweep.class)))
                .thenAnswer(invocation -> {
                    Sweep sweep = invocation.getArgument(0);
                    setField(sweep, "sweepRequestId", 1L);
                    return sweep;
                });

        when(sweepOutboxRepository.save(any(SweepOutbox.class)))
                .thenAnswer(invocation -> {
                    SweepOutbox outbox = invocation.getArgument(0);
                    setField(outbox, "outboxEventId", 1L);
                    return outbox;
                });

        // when
        SweepRequestCreateResponse response = autoSweepRequestService.createSweepRequest(request);

        // then
        assertThat(response.sweepRequestId()).isEqualTo(1L);
        assertThat(response.requestStatus()).isEqualTo(SweepProcessStatus.PENDING_PUBLISH.name());
        assertThat(response.pointLedgerId()).isEqualTo(1L);
        assertThat(response.krwAmount()).isEqualTo(12450L);
        assertThat(response.etfId()).isEqualTo(100L);

        verify(cardCoreRewardSweepApi).requestSweep(cardUserUuid, 1L);
        verify(sweepRepository).save(any(Sweep.class));
        verify(sweepOutboxRepository).save(any(SweepOutbox.class));
    }

    @Test
    @DisplayName("outbox payload에는 Core 응답 기반 SWEEP_REQUESTED 이벤트 정보가 포함된다")
    void createSweepRequestOutboxPayloadContainsEventData() {
        // given
        AutoSweepCreateCommand request = createSweepRequestRequest(2L);

        when(sweepRepository.existsByPointLedgerId(2L)).thenReturn(false);
        when(sweepRepository.existsByIdempotencyKey("SWEEP:POINT_LEDGER:2")).thenReturn(false);

        when(cardCoreRewardSweepApi.requestSweep(cardUserUuid, 2L))
                .thenReturn(ApiResponse.of(SuccessStatus.OK, createSweepRequestCoreResponse(2L, 9800L)));

        when(sweepRepository.save(any(Sweep.class)))
                .thenAnswer(invocation -> {
                    Sweep sweep = invocation.getArgument(0);
                    setField(sweep, "sweepRequestId", 2L);
                    return sweep;
                });

        when(sweepOutboxRepository.save(any(SweepOutbox.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<SweepOutbox> outboxCaptor = ArgumentCaptor.forClass(SweepOutbox.class);

        // when
        autoSweepRequestService.createSweepRequest(request);

        // then
        verify(sweepOutboxRepository).save(outboxCaptor.capture());

        SweepOutbox outbox = outboxCaptor.getValue();

        assertThat(outbox.getSweepRequestId()).isEqualTo(2L);
        assertThat(outbox.getEventId()).startsWith("CARD-SWEEP-");
        assertThat(outbox.getEventType()).isEqualTo(SweepEventType.SWEEP_REQUESTED);
        assertThat(outbox.getPublishStatus()).isEqualTo(OutboxPublishStatus.PENDING);
        assertThat(outbox.getIdempotencyKey()).isEqualTo("SWEEP:POINT_LEDGER:2");
        assertThat(outbox.getPayload()).contains("\"eventType\":\"SWEEP_REQUESTED\"");
        assertThat(outbox.getPayload()).contains("\"sweepRequestId\":2");
        assertThat(outbox.getPayload()).contains("\"pointLedgerId\":2");
        assertThat(outbox.getPayload()).contains("\"baseMonth\":\"2026-05\"");
        assertThat(outbox.getPayload()).contains("\"krwAmount\":9800");
    }

    @Test
    @DisplayName("이미 같은 pointLedgerId 요청이 있으면 Core를 호출하지 않고 예외가 발생한다")
    void createSweepRequestDuplicatePointLedgerId() {
        // given
        AutoSweepCreateCommand request = createSweepRequestRequest(1L);

        when(sweepRepository.existsByPointLedgerId(1L)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> autoSweepRequestService.createSweepRequest(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", SweepErrorCode.SWEEP_ALREADY_REQUESTED);

        verify(cardCoreRewardSweepApi, never()).requestSweep(any(), any());
        verify(sweepRepository, never()).save(any());
        verify(sweepOutboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 같은 idempotencyKey 요청이 있으면 Core를 호출하지 않고 예외가 발생한다")
    void createSweepRequestDuplicateIdempotencyKey() {
        // given
        AutoSweepCreateCommand request = createSweepRequestRequest(1L);

        when(sweepRepository.existsByPointLedgerId(1L)).thenReturn(false);
        when(sweepRepository.existsByIdempotencyKey("SWEEP:POINT_LEDGER:1")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> autoSweepRequestService.createSweepRequest(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", SweepErrorCode.SWEEP_ALREADY_REQUESTED);

        verify(cardCoreRewardSweepApi, never()).requestSweep(any(), any());
        verify(sweepRepository, never()).save(any());
        verify(sweepOutboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("DB unique 제약 위반이 발생하면 중복 요청 예외로 변환한다")
    void createSweepRequestDataIntegrityViolation() {
        // given
        AutoSweepCreateCommand request = createSweepRequestRequest(1L);

        when(sweepRepository.existsByPointLedgerId(1L)).thenReturn(false);
        when(sweepRepository.existsByIdempotencyKey("SWEEP:POINT_LEDGER:1")).thenReturn(false);

        when(cardCoreRewardSweepApi.requestSweep(cardUserUuid, 1L))
                .thenReturn(ApiResponse.of(SuccessStatus.OK, createSweepRequestCoreResponse(1L, 12450L)));

        when(sweepRepository.save(any(Sweep.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        // when & then
        assertThatThrownBy(() -> autoSweepRequestService.createSweepRequest(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", SweepErrorCode.SWEEP_REQUEST_SAVE_FAILED);

        verify(sweepOutboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("request가 null이면 잘못된 요청 예외가 발생한다")
    void createSweepRequestNullRequest() {
        assertThatThrownBy(() -> autoSweepRequestService.createSweepRequest(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", SweepErrorCode.SWEEP_INVALID_REQUEST);

        verify(cardCoreRewardSweepApi, never()).requestSweep(any(), any());
        verify(sweepRepository, never()).save(any());
        verify(sweepOutboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Core 응답 data가 null이면 예외가 발생한다")
    void createSweepRequestCoreResponseDataNull() {
        // given
        AutoSweepCreateCommand request = createSweepRequestRequest(1L);

        when(sweepRepository.existsByPointLedgerId(1L)).thenReturn(false);
        when(sweepRepository.existsByIdempotencyKey("SWEEP:POINT_LEDGER:1")).thenReturn(false);
        when(cardCoreRewardSweepApi.requestSweep(cardUserUuid, 1L))
                .thenReturn(ApiResponse.of(SuccessStatus.OK, null));

        // when & then
        assertThatThrownBy(() -> autoSweepRequestService.createSweepRequest(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", SweepErrorCode.SWEEP_CORE_RESPONSE_INVALID);

        verify(sweepRepository, never()).save(any());
        verify(sweepOutboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Core 응답 금액이 0이면 잘못된 요청 예외가 발생한다")
    void createSweepRequestCoreResponseInvalidAmount() {
        // given
        AutoSweepCreateCommand request = createSweepRequestRequest(1L);

        when(sweepRepository.existsByPointLedgerId(1L)).thenReturn(false);
        when(sweepRepository.existsByIdempotencyKey("SWEEP:POINT_LEDGER:1")).thenReturn(false);
        when(cardCoreRewardSweepApi.requestSweep(cardUserUuid, 1L))
                .thenReturn(ApiResponse.of(SuccessStatus.OK, createSweepRequestCoreResponse(1L, 0L)));

        // when & then
        assertThatThrownBy(() -> autoSweepRequestService.createSweepRequest(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", SweepErrorCode.SWEEP_INVALID_REQUEST);

        verify(sweepRepository, never()).save(any());
        verify(sweepOutboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Core 선점 후 sweep_request 저장에 실패하면 Core 스윕 보상 API를 호출한다")
    void createSweepRequestCompensatesWhenSweepRequestSaveFails() {
        // given
        AutoSweepCreateCommand request = createSweepRequestRequest(1L);

        when(sweepRepository.existsByPointLedgerId(1L)).thenReturn(false);
        when(sweepRepository.existsByIdempotencyKey("SWEEP:POINT_LEDGER:1")).thenReturn(false);

        when(cardCoreRewardSweepApi.requestSweep(cardUserUuid, 1L))
                .thenReturn(ApiResponse.of(SuccessStatus.OK, createSweepRequestCoreResponse(1L, 12450L)));

        when(sweepRepository.save(any(Sweep.class)))
                .thenThrow(new DataIntegrityViolationException("save failed"));

        // when & then
        assertThatThrownBy(() -> autoSweepRequestService.createSweepRequest(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", SweepErrorCode.SWEEP_REQUEST_SAVE_FAILED);

        verify(cardCoreRewardSweepApi).cancelSweepRequest(cardUserUuid, 1L);
        verify(sweepOutboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Core 선점 후 outbox 저장에 실패하면 Core 스윕 보상 API를 호출한다")
    void createSweepRequestCompensatesWhenOutboxSaveFails() {
        // given
        AutoSweepCreateCommand request = createSweepRequestRequest(1L);

        when(sweepRepository.existsByPointLedgerId(1L)).thenReturn(false);
        when(sweepRepository.existsByIdempotencyKey("SWEEP:POINT_LEDGER:1")).thenReturn(false);

        when(cardCoreRewardSweepApi.requestSweep(cardUserUuid, 1L))
                .thenReturn(ApiResponse.of(SuccessStatus.OK, createSweepRequestCoreResponse(1L, 12450L)));

        when(sweepRepository.save(any(Sweep.class)))
                .thenAnswer(invocation -> {
                    Sweep sweep = invocation.getArgument(0);
                    setField(sweep, "sweepRequestId", 1L);
                    return sweep;
                });

        when(sweepOutboxRepository.save(any(SweepOutbox.class)))
                .thenThrow(new DataIntegrityViolationException("outbox save failed"));

        // when & then
        assertThatThrownBy(() -> autoSweepRequestService.createSweepRequest(request))
                .isInstanceOf(BusinessException.class);

        verify(cardCoreRewardSweepApi).cancelSweepRequest(cardUserUuid, 1L);
    }

    @Test
    @DisplayName("Core 선점 후 Core 응답 값 검증에 실패하면 Core 스윕 보상 API를 호출한다")
    void createSweepRequestCompensatesWhenCoreResponseInvalidAfterReservation() {
        // given
        AutoSweepCreateCommand request = createSweepRequestRequest(1L);

        when(sweepRepository.existsByPointLedgerId(1L)).thenReturn(false);
        when(sweepRepository.existsByIdempotencyKey("SWEEP:POINT_LEDGER:1")).thenReturn(false);

        when(cardCoreRewardSweepApi.requestSweep(cardUserUuid, 1L))
                .thenReturn(ApiResponse.of(SuccessStatus.OK, createSweepRequestCoreResponse(1L, 0L)));

        // when & then
        assertThatThrownBy(() -> autoSweepRequestService.createSweepRequest(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", SweepErrorCode.SWEEP_INVALID_REQUEST);

        verify(cardCoreRewardSweepApi).cancelSweepRequest(cardUserUuid, 1L);
        verify(sweepRepository, never()).save(any());
        verify(sweepOutboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Core 보상 API 호출이 실패해도 원래 Channel 예외를 유지한다")
    void createSweepRequestKeepsOriginalExceptionWhenCompensationFails() {
        // given
        AutoSweepCreateCommand request = createSweepRequestRequest(1L);

        when(sweepRepository.existsByPointLedgerId(1L)).thenReturn(false);
        when(sweepRepository.existsByIdempotencyKey("SWEEP:POINT_LEDGER:1")).thenReturn(false);

        when(cardCoreRewardSweepApi.requestSweep(cardUserUuid, 1L))
                .thenReturn(ApiResponse.of(SuccessStatus.OK, createSweepRequestCoreResponse(1L, 12450L)));

        when(sweepRepository.save(any(Sweep.class)))
                .thenThrow(new DataIntegrityViolationException("save failed"));

        doThrow(new RuntimeException("cancel failed"))
                .when(cardCoreRewardSweepApi)
                .cancelSweepRequest(cardUserUuid, 1L);

        // when & then
        assertThatThrownBy(() -> autoSweepRequestService.createSweepRequest(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", SweepErrorCode.SWEEP_REQUEST_SAVE_FAILED);

        verify(cardCoreRewardSweepApi).cancelSweepRequest(cardUserUuid, 1L);
    }

    @Test
    @DisplayName("Core 선점 자체가 실패하면 Core 보상 API를 호출하지 않는다")
    void createSweepRequestDoesNotCompensateWhenCoreReservationFails() {
        // given
        AutoSweepCreateCommand request = createSweepRequestRequest(1L);

        when(sweepRepository.existsByPointLedgerId(1L)).thenReturn(false);
        when(sweepRepository.existsByIdempotencyKey("SWEEP:POINT_LEDGER:1")).thenReturn(false);

        when(cardCoreRewardSweepApi.requestSweep(cardUserUuid, 1L))
                .thenReturn(ApiResponse.of(SuccessStatus.OK, null));

        // when & then
        assertThatThrownBy(() -> autoSweepRequestService.createSweepRequest(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", SweepErrorCode.SWEEP_CORE_RESPONSE_INVALID);

        verify(cardCoreRewardSweepApi, never()).cancelSweepRequest(any(), any());
        verify(sweepRepository, never()).save(any());
        verify(sweepOutboxRepository, never()).save(any());
    }

    private AutoSweepCreateCommand createSweepRequestRequest(Long pointLedgerId) {
        return new AutoSweepCreateCommand(
                userUuid,
                cardUserUuid,
                pointLedgerId,
                100L
        );
    }

    private CardCoreSweepRequestResponse createSweepRequestCoreResponse(Long pointLedgerId, Long amount) {
        return new CardCoreSweepRequestResponse(
                pointLedgerId,
                10L,
                "2026-05",
                amount,
                amount,
                "REQUESTED"
        );
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
