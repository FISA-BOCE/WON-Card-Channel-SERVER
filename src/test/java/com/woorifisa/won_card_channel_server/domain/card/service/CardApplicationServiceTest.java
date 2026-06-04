package com.woorifisa.won_card_channel_server.domain.card.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.domain.auth.model.CardChnAuthUser;
import com.woorifisa.won_card_channel_server.domain.auth.model.UserStatus;
import com.woorifisa.won_card_channel_server.domain.auth.repository.CardChnAuthUserRepository;
import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.InvestEtfDetailsResponse;
import com.woorifisa.won_card_channel_server.domain.autoinvest.external.InvestChannelAutoInvestApi;
import com.woorifisa.won_card_channel_server.domain.autoinvest.service.AutoInvestSubscriptionService;
import com.woorifisa.won_card_channel_server.domain.autoinvest.service.validate.InvestEtfResponseValidator;
import com.woorifisa.won_card_channel_server.domain.card.dto.request.CardApplicationCreateRequest;
import com.woorifisa.won_card_channel_server.domain.card.dto.request.CardCoreApplicationRequest;
import com.woorifisa.won_card_channel_server.domain.card.dto.response.CardApplicationCreateResponse;
import com.woorifisa.won_card_channel_server.domain.card.dto.response.CardCoreApplicationResponse;
import com.woorifisa.won_card_channel_server.domain.card.exception.code.CardErrorCode;
import com.woorifisa.won_card_channel_server.domain.card.external.CardCoreCardApplicationApi;
import com.woorifisa.won_card_channel_server.domain.card.repository.CardChnCardSummaryRepository;
import com.woorifisa.won_card_channel_server.domain.user.dto.request.UpdateCardUserMappingRequest;
import com.woorifisa.won_card_channel_server.domain.user.dto.response.GetMyUserMappingResponse;
import com.woorifisa.won_card_channel_server.domain.user.external.CommonUserMappingApi;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import com.woorifisa.won_card_channel_server.global.security.TextEncryptor;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;

class CardApplicationServiceTest {

    private final UUID userUuid = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private final UUID authUserUuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID investAccountUuid = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private final UUID cardUuid = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private CardChnAuthUserRepository authUserRepository;
    private CardChnCardSummaryRepository cardSummaryRepository;
    private CardCoreCardApplicationApi cardCoreCardApplicationApi;
    private InvestChannelAutoInvestApi investChannelAutoInvestApi;
    private AutoInvestSubscriptionService autoInvestSubscriptionService;
    private CommonUserMappingApi commonUserMappingApi;
    private InvestEtfResponseValidator investEtfResponseValidator;
    private TextEncryptor textEncryptor;
    private CardApplicationService service;

    @BeforeEach
    void setUp() {
        authUserRepository = mock(CardChnAuthUserRepository.class);
        cardSummaryRepository = mock(CardChnCardSummaryRepository.class);
        cardCoreCardApplicationApi = mock(CardCoreCardApplicationApi.class);
        investChannelAutoInvestApi = mock(InvestChannelAutoInvestApi.class);
        autoInvestSubscriptionService = mock(AutoInvestSubscriptionService.class);
        commonUserMappingApi = mock(CommonUserMappingApi.class);
        investEtfResponseValidator = mock(InvestEtfResponseValidator.class);
        textEncryptor = mock(TextEncryptor.class);
        service = new CardApplicationService(
                authUserRepository,
                cardSummaryRepository,
                cardCoreCardApplicationApi,
                investChannelAutoInvestApi,
                autoInvestSubscriptionService,
                commonUserMappingApi,
                investEtfResponseValidator,
                textEncryptor,
                new ObjectMapper()
        );
        given(textEncryptor.encrypt(anyString())).willAnswer(invocation -> "cipher:" + invocation.getArgument(0, String.class));
        given(investEtfResponseValidator.validateForAutoInvest(eq(1001L), eq("VOO"), any()))
                .willReturn(new InvestEtfDetailsResponse(1001L, "S&P 500 ETF", "VOO", true, true));
    }

    @Test
    @DisplayName("카드 신청 성공 시 카드 코어 발급 후 자동투자 초기 설정을 생성한다")
    void applyCardSuccess() {
        CardApplicationCreateRequest request = request();

        given(authUserRepository.findByUserUuid(userUuid)).willReturn(Optional.of(activeAuthUser()));
        given(commonUserMappingApi.getMappingStatus(userUuid))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        mappingResponse(userUuid, null, false,
                                UUID.fromString("66666666-6666-6666-6666-666666666666"), true)));
        given(investChannelAutoInvestApi.getInvestmentAccount(userUuid, investAccountUuid))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        new com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.InvestAccountDetailsResponse(
                                investAccountUuid, userUuid, "ACTIVE"
                        )));
        given(investChannelAutoInvestApi.getEtf(1001L))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        new InvestEtfDetailsResponse(1001L, "S&P 500 ETF", "VOO", true, true)));
        given(cardCoreCardApplicationApi.applyCard(eq(userUuid), any(CardCoreApplicationRequest.class)))
                .willReturn(ApiResponse.of(SuccessStatus.CARD_APPLICATION_CREATED,
                        new CardCoreApplicationResponse(cardUuid, "****-****-****-1234",
                                LocalDateTime.of(2026, 5, 28, 17, 0), "ACTIVE")));
        CardApplicationCreateResponse response = service.applyCard(authenticatedUser(), request);

        assertThat(response.cardUuid()).isEqualTo(cardUuid);
        assertThat(response.autoInvestEtfName()).isEqualTo("S&P 500 ETF");
        verify(textEncryptor, times(5)).encrypt(anyString());
        verify(cardCoreCardApplicationApi).applyCard(eq(userUuid), argThat(coreRequest ->
                "cipher:홍길동".equals(coreRequest.userNameEnc())
                        && "cipher:19900101".equals(coreRequest.birthDateEnc())
                        && "cipher:01012345678".equals(coreRequest.telEnc())
                        && "cipher:test@example.com".equals(coreRequest.emailEnc())
                        && "cipher:서울시 마포구 상암동".equals(coreRequest.addressEnc())
        ));
        verify(autoInvestSubscriptionService).createInitialSubscription(userUuid, investAccountUuid, 1001L, "VOO");
    }

    @Test
    @DisplayName("카드 사용자 UUID가 있으면 카드 요약 projection에 선택 ETF ID를 저장한다")
    void applyCardSavesSelectedEtfIdInCardSummary() {
        CardApplicationCreateRequest request = request();

        given(authUserRepository.findByUserUuid(userUuid)).willReturn(Optional.of(activeAuthUserWithCardUser()));
        given(commonUserMappingApi.getMappingStatus(userUuid))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        mappingResponse(userUuid, null, false,
                                UUID.fromString("66666666-6666-6666-6666-666666666666"), true)));
        given(investChannelAutoInvestApi.getInvestmentAccount(userUuid, investAccountUuid))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        new com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.InvestAccountDetailsResponse(
                                investAccountUuid, userUuid, "ACTIVE"
                        )));
        given(investChannelAutoInvestApi.getEtf(1001L))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        new InvestEtfDetailsResponse(1001L, "S&P 500 ETF", "VOO", true, true)));
        given(cardCoreCardApplicationApi.applyCard(eq(userUuid), any(CardCoreApplicationRequest.class)))
                .willReturn(ApiResponse.of(SuccessStatus.CARD_APPLICATION_CREATED,
                        new CardCoreApplicationResponse(cardUuid, "****-****-****-1234",
                                LocalDateTime.of(2026, 5, 28, 17, 0), "ACTIVE")));
        given(commonUserMappingApi.updateCardUserMapping(eq(userUuid), any(UpdateCardUserMappingRequest.class)))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        mappingResponse(
                                userUuid,
                                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                                true,
                                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                                true
                        )));
        service.applyCard(authenticatedUser(), request);

        verify(commonUserMappingApi).updateCardUserMapping(eq(userUuid), any(UpdateCardUserMappingRequest.class));
        verify(cardSummaryRepository).save(argThat(summary ->
                summary.getSelectedEtfId() != null && summary.getSelectedEtfId().equals(1001L)
        ));
    }

    @Test
    @DisplayName("카드 코어 발급 실패 시 자동투자 초기 설정을 생성하지 않는다")
    void applyCardWhenCoreFails() {
        CardApplicationCreateRequest request = request();

        given(authUserRepository.findByUserUuid(userUuid)).willReturn(Optional.of(activeAuthUser()));
        given(commonUserMappingApi.getMappingStatus(userUuid))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        mappingResponse(userUuid, null, false,
                                UUID.fromString("66666666-6666-6666-6666-666666666666"), true)));
        given(investChannelAutoInvestApi.getInvestmentAccount(userUuid, investAccountUuid))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        new com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.InvestAccountDetailsResponse(
                                investAccountUuid, userUuid, "ACTIVE"
                        )));
        given(investChannelAutoInvestApi.getEtf(1001L))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        new InvestEtfDetailsResponse(1001L, "S&P 500 ETF", "VOO", true, true)));
        given(cardCoreCardApplicationApi.applyCard(eq(userUuid), any(CardCoreApplicationRequest.class)))
                .willThrow(feignException(409, "{\"code\":\"CARD_409_001\"}"));

        assertThatThrownBy(() -> service.applyCard(authenticatedUser(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CardErrorCode.CARD_ALREADY_EXISTS);

        verify(autoInvestSubscriptionService, never()).createInitialSubscription(any(), any(), any(), any());
    }

    @Test
    @DisplayName("필수 약관이 누락되면 예외가 발생한다")
    void applyCardWithoutRequiredTerms() {
        CardApplicationCreateRequest request = new CardApplicationCreateRequest(
                new CardApplicationCreateRequest.ApplicantInfo(
                        "홍길동",
                        "HONG GIL DONG",
                        "19900101",
                        "M",
                        "KR",
                        "01012345678",
                        "test@example.com",
                        "서울시 마포구 상암동",
                        "직장인"
                ),
                investAccountUuid, 1001L, "VOO",
                false,
                null
        );

        assertThatThrownBy(() -> service.applyCard(authenticatedUser(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CardErrorCode.CARD_APPLICATION_TERMS_NOT_AGREED);
    }

    @Test
    @DisplayName("카드 코어가 입력값 오류를 반환하면 입력값 오류로 매핑한다")
    void applyCardWhenCoreReturnsInvalidRequest() {
        CardApplicationCreateRequest request = request();

        given(authUserRepository.findByUserUuid(userUuid)).willReturn(Optional.of(activeAuthUser()));
        given(commonUserMappingApi.getMappingStatus(userUuid))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        mappingResponse(userUuid, null, false,
                                UUID.fromString("66666666-6666-6666-6666-666666666666"), true)));
        given(investChannelAutoInvestApi.getInvestmentAccount(userUuid, investAccountUuid))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        new com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.InvestAccountDetailsResponse(
                                investAccountUuid, userUuid, "ACTIVE"
                        )));
        given(investChannelAutoInvestApi.getEtf(1001L))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        new InvestEtfDetailsResponse(1001L, "S&P 500 ETF", "VOO", true, true)));
        given(cardCoreCardApplicationApi.applyCard(eq(userUuid), any(CardCoreApplicationRequest.class)))
                .willThrow(feignException(400, "{\"code\":\"CARD_400_001\"}"));

        assertThatThrownBy(() -> service.applyCard(authenticatedUser(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CardErrorCode.CARD_APPLICATION_INVALID_REQUEST);
    }

    @Test
    @DisplayName("카드 코어가 필수 약관 미동의를 반환하면 약관 미동의로 매핑한다")
    void applyCardWhenCoreReturnsTermsNotAgreed() {
        CardApplicationCreateRequest request = request();

        given(authUserRepository.findByUserUuid(userUuid)).willReturn(Optional.of(activeAuthUser()));
        given(commonUserMappingApi.getMappingStatus(userUuid))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        mappingResponse(userUuid, null, false,
                                UUID.fromString("66666666-6666-6666-6666-666666666666"), true)));
        given(investChannelAutoInvestApi.getInvestmentAccount(userUuid, investAccountUuid))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        new com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.InvestAccountDetailsResponse(
                                investAccountUuid, userUuid, "ACTIVE"
                        )));
        given(investChannelAutoInvestApi.getEtf(1001L))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        new InvestEtfDetailsResponse(1001L, "S&P 500 ETF", "VOO", true, true)));
        given(cardCoreCardApplicationApi.applyCard(eq(userUuid), any(CardCoreApplicationRequest.class)))
                .willThrow(feignException(400, "{\"code\":\"CARD_400_002\"}"));

        assertThatThrownBy(() -> service.applyCard(authenticatedUser(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CardErrorCode.CARD_APPLICATION_TERMS_NOT_AGREED);
    }

    @Test
    @DisplayName("인증 사용자 정보가 없으면 예외가 발생한다")
    void applyCardWithoutAuthenticatedUser() {
        assertThatThrownBy(() -> service.applyCard(null, request()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.AUTHENTICATION_REQUIRED);
    }

    @Test
    @DisplayName("cardUserUuid가 없으면 common server 카드 매핑 PATCH는 호출하지 않는다")
    void applyCardSkipsCardMappingPatchWhenCardUserUuidMissing() {
        CardApplicationCreateRequest request = request();

        given(authUserRepository.findByUserUuid(userUuid)).willReturn(Optional.of(activeAuthUser()));
        given(commonUserMappingApi.getMappingStatus(userUuid))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        mappingResponse(userUuid, null, false,
                                UUID.fromString("66666666-6666-6666-6666-666666666666"), true)));
        given(investChannelAutoInvestApi.getInvestmentAccount(userUuid, investAccountUuid))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        new com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.InvestAccountDetailsResponse(
                                investAccountUuid, userUuid, "ACTIVE"
                        )));
        given(investChannelAutoInvestApi.getEtf(1001L))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        new InvestEtfDetailsResponse(1001L, "S&P 500 ETF", "VOO", true, true)));
        given(cardCoreCardApplicationApi.applyCard(eq(userUuid), any(CardCoreApplicationRequest.class)))
                .willReturn(ApiResponse.of(SuccessStatus.CARD_APPLICATION_CREATED,
                        new CardCoreApplicationResponse(cardUuid, "****-****-****-1234",
                                LocalDateTime.of(2026, 5, 28, 17, 0), "ACTIVE")));

        service.applyCard(authenticatedUser(), request);

        verify(commonUserMappingApi, never()).updateCardUserMapping(any(), any());
    }

    @Test
    @DisplayName("투자 서비스 매핑이 없으면 카드 신청을 차단한다")
    void applyCardWithoutInvestMapping() {
        given(authUserRepository.findByUserUuid(userUuid)).willReturn(Optional.of(activeAuthUser()));
        given(commonUserMappingApi.getMappingStatus(userUuid))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        mappingResponse(userUuid, null, false, null, false)));

        assertThatThrownBy(() -> service.applyCard(authenticatedUser(), request()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CardErrorCode.CARD_INVEST_LINK_REQUIRED);

        verify(investChannelAutoInvestApi, never()).getInvestmentAccount(any(), any());
        verify(cardCoreCardApplicationApi, never()).applyCard(any(), any());
        verify(autoInvestSubscriptionService, never()).createInitialSubscription(any(), any(), any(), any());
    }

    @Test
    @DisplayName("카드 연결 상태가 true인데 cardUserUuid가 없으면 잘못된 매핑 응답으로 처리한다")
    void applyCardWithConnectedCardButMissingCardUserUuid() {
        given(authUserRepository.findByUserUuid(userUuid)).willReturn(Optional.of(activeAuthUser()));
        given(commonUserMappingApi.getMappingStatus(userUuid))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        new GetMyUserMappingResponse(
                                userUuid,
                                new GetMyUserMappingResponse.CardMapping(null, true),
                                new GetMyUserMappingResponse.InvestMapping(
                                        UUID.fromString("66666666-6666-6666-6666-666666666666"),
                                        true
                                )
                        )));

        assertThatThrownBy(() -> service.applyCard(authenticatedUser(), request()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CardErrorCode.CARD_MAPPING_RESPONSE_INVALID);

        verify(investChannelAutoInvestApi, never()).getInvestmentAccount(any(), any());
        verify(cardCoreCardApplicationApi, never()).applyCard(any(), any());
        verify(autoInvestSubscriptionService, never()).createInitialSubscription(any(), any(), any(), any());
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(authUserUuid, userUuid, "test-jti");
    }

    private CardChnAuthUser activeAuthUser() {
        return CardChnAuthUser.builder()
                .authUserUuid(authUserUuid)
                .userUuid(userUuid)
                .loginId("login")
                .passwordHash("pw")
                .userStatus(UserStatus.ACTIVE)
                .build();
    }

    private CardChnAuthUser activeAuthUserWithCardUser() {
        return CardChnAuthUser.builder()
                .authUserUuid(authUserUuid)
                .cardUserUuid(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .userUuid(userUuid)
                .loginId("login")
                .passwordHash("pw")
                .userStatus(UserStatus.ACTIVE)
                .build();
    }

    private CardApplicationCreateRequest request() {
        return new CardApplicationCreateRequest(
                new CardApplicationCreateRequest.ApplicantInfo(
                        "홍길동",
                        "HONG GIL DONG",
                        "19900101",
                        "M",
                        "KR",
                        "01012345678",
                        "test@example.com",
                        "서울시 마포구 상암동",
                        "직장인"
                ),
                investAccountUuid,
                1001L,
                "VOO",
                true,
                new CardApplicationCreateRequest.OptionalTerms(false, false)
        );
    }

    private FeignException feignException(int status, String body) {
        Request request = Request.create(
                Request.HttpMethod.POST,
                "http://localhost/test",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                null
        );
        return FeignException.errorStatus(
                "applyCard",
                Response.builder()
                        .status(status)
                        .reason("error")
                        .request(request)
                        .headers(Map.of())
                        .body(body, StandardCharsets.UTF_8)
                        .build()
        );
    }

    private GetMyUserMappingResponse mappingResponse(
            UUID userUuid,
            UUID cardUserUuid,
            boolean cardConnected,
            UUID investUserUuid,
            boolean investConnected
    ) {
        return new GetMyUserMappingResponse(
                userUuid,
                cardMapping(cardUserUuid, cardConnected),
                investMapping(investUserUuid, investConnected)
        );
    }

    private GetMyUserMappingResponse.CardMapping cardMapping(UUID cardUserUuid, boolean connected) {
        if (cardUserUuid == null && !connected) {
            return null;
        }
        return new GetMyUserMappingResponse.CardMapping(cardUserUuid, connected);
    }

    private GetMyUserMappingResponse.InvestMapping investMapping(UUID investUserUuid, boolean connected) {
        if (investUserUuid == null && !connected) {
            return null;
        }
        return new GetMyUserMappingResponse.InvestMapping(investUserUuid, connected);
    }
}
