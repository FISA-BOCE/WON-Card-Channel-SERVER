package com.woorifisa.won_card_channel_server.domain.reward.service;

import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.domain.auth.model.CardChnAuthUser;
import com.woorifisa.won_card_channel_server.domain.auth.repository.CardChnAuthUserRepository;
import com.woorifisa.won_card_channel_server.domain.card.exception.code.CardErrorCode;
import com.woorifisa.won_card_channel_server.domain.reward.dto.response.CardCoreRewardLedgerResponse;
import com.woorifisa.won_card_channel_server.domain.reward.dto.response.RewardLedgerResponse;
import com.woorifisa.won_card_channel_server.domain.reward.exception.code.RewardErrorCode;
import com.woorifisa.won_card_channel_server.domain.reward.external.CardCoreRewardApi;
import com.woorifisa.won_card_channel_server.domain.reward.mapper.RewardLedgerMapper;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RewardLedgerServiceTest {

    private static final UUID USER_UUID =
            UUID.fromString("0a31e4b1-2b1d-4b5e-8b82-0fb48e502111");

    private static final UUID AUTH_USER_UUID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID CARD_USER_UUID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private CardChnAuthUserRepository userRepository;

    @Mock
    private CardCoreRewardApi cardCoreRewardApi;

    @Mock
    private RewardLedgerMapper rewardLedgerMapper;

    @InjectMocks
    private RewardLedgerService rewardLedgerService;

    @Test
    @DisplayName("자동투자 리워드 내역을 조회한다")
    void getRewardLedger() throws Exception {
        // given
        AuthenticatedUser authenticatedUser = authenticatedUser();
        CardChnAuthUser user = newAuthUser(USER_UUID, CARD_USER_UUID);

        CardCoreRewardLedgerResponse coreResponse = new CardCoreRewardLedgerResponse(
                2026,
                1245000L,
                List.of(
                        new CardCoreRewardLedgerResponse.CardCoreRewardLedgerItem(
                                1001L,
                                "2026-05",
                                12450L,
                                "EARN",
                                LocalDateTime.of(2026, 5, 7, 14, 32)
                        )
                )
        );

        RewardLedgerResponse response = new RewardLedgerResponse(
                2026,
                1245000L,
                List.of(
                        new RewardLedgerResponse.RewardLedgerItem(
                                1001L,
                                "2026-05",
                                12450L,
                                "EARN",
                                LocalDateTime.of(2026, 5, 7, 14, 32)
                        )
                )
        );

        given(userRepository.findByUserUuid(USER_UUID))
                .willReturn(Optional.of(user));

        given(cardCoreRewardApi.getRewardLedger(CARD_USER_UUID, "EARN"))
                .willReturn(ApiResponse.of(SuccessStatus.OK, coreResponse));

        given(rewardLedgerMapper.toResponse(coreResponse))
                .willReturn(response);

        // when
        RewardLedgerResponse result = rewardLedgerService.getRewardLedger(authenticatedUser, "EARN");

        // then
        assertThat(result.baseYear()).isEqualTo(2026);
        assertThat(result.totalAccumulatedAmount()).isEqualTo(1245000L);
        assertThat(result.ledgers()).hasSize(1);
        assertThat(result.ledgers().get(0).pointLedgerId()).isEqualTo(1001L);

        then(userRepository).should().findByUserUuid(USER_UUID);
        then(cardCoreRewardApi).should().getRewardLedger(CARD_USER_UUID, "EARN");
        then(rewardLedgerMapper).should().toResponse(coreResponse);
    }

    @Test
    @DisplayName("type 파라미터가 없으면 ALL로 조회한다")
    void getRewardLedgerWithoutType() throws Exception {
        // given
        AuthenticatedUser authenticatedUser = authenticatedUser();
        CardChnAuthUser user = newAuthUser(USER_UUID, CARD_USER_UUID);

        CardCoreRewardLedgerResponse coreResponse = new CardCoreRewardLedgerResponse(
                2026,
                1245000L,
                List.of()
        );

        RewardLedgerResponse response = new RewardLedgerResponse(
                2026,
                1245000L,
                List.of()
        );

        given(userRepository.findByUserUuid(USER_UUID))
                .willReturn(Optional.of(user));

        given(cardCoreRewardApi.getRewardLedger(CARD_USER_UUID, "ALL"))
                .willReturn(ApiResponse.of(SuccessStatus.OK, coreResponse));

        given(rewardLedgerMapper.toResponse(coreResponse))
                .willReturn(response);

        // when
        RewardLedgerResponse result = rewardLedgerService.getRewardLedger(authenticatedUser, null);

        // then
        assertThat(result.baseYear()).isEqualTo(2026);
        assertThat(result.totalAccumulatedAmount()).isEqualTo(1245000L);
        assertThat(result.ledgers()).isEmpty();

        then(cardCoreRewardApi).should().getRewardLedger(CARD_USER_UUID, "ALL");
    }

    @Test
    @DisplayName("카드 사용자 정보가 없으면 예외가 발생한다")
    void getRewardLedgerCardUserNotFound() {
        // given
        AuthenticatedUser authenticatedUser = authenticatedUser();

        given(userRepository.findByUserUuid(USER_UUID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> rewardLedgerService.getRewardLedger(authenticatedUser, "EARN"))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode())
                            .isEqualTo(CardErrorCode.CARD_USER_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("cardUserUuid가 null이면 예외가 발생한다")
    void getRewardLedgerCardUserUuidNull() throws Exception {
        // given
        AuthenticatedUser authenticatedUser = authenticatedUser();
        CardChnAuthUser user = newAuthUser(USER_UUID, null);

        given(userRepository.findByUserUuid(USER_UUID))
                .willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> rewardLedgerService.getRewardLedger(authenticatedUser, "EARN"))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode())
                            .isEqualTo(CardErrorCode.CARD_USER_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("유효하지 않은 type 값이면 예외가 발생한다")
    void getRewardLedgerInvalidType() {
        // given
        AuthenticatedUser authenticatedUser = authenticatedUser();

        String invalidType = "BAD";

        // when & then
        assertThatThrownBy(() -> rewardLedgerService.getRewardLedger(authenticatedUser, invalidType))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode())
                            .isEqualTo(RewardErrorCode.INVALID_REWARD_LEDGER_TYPE);
                });
    }

    @Test
    @DisplayName("Card Core 응답 자체가 null이면 예외가 발생한다")
    void getRewardLedgerCoreResponseNull() throws Exception {
        // given
        AuthenticatedUser authenticatedUser = authenticatedUser();
        CardChnAuthUser user = newAuthUser(USER_UUID, CARD_USER_UUID);

        given(userRepository.findByUserUuid(USER_UUID))
                .willReturn(Optional.of(user));

        given(cardCoreRewardApi.getRewardLedger(CARD_USER_UUID, "EARN"))
                .willReturn(null);

        // when & then
        assertThatThrownBy(() -> rewardLedgerService.getRewardLedger(authenticatedUser, "EARN"))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode())
                            .isEqualTo(RewardErrorCode.INVALID_CORE_REWARD_RESPONSE);
                });
    }

    @Test
    @DisplayName("Card Core 응답 data가 null이면 예외가 발생한다")
    void getRewardLedgerCoreResponseDataNull() throws Exception {
        // given
        AuthenticatedUser authenticatedUser = authenticatedUser();
        CardChnAuthUser user = newAuthUser(USER_UUID, CARD_USER_UUID);

        given(userRepository.findByUserUuid(USER_UUID))
                .willReturn(Optional.of(user));

        given(cardCoreRewardApi.getRewardLedger(CARD_USER_UUID, "EARN"))
                .willReturn(ApiResponse.of(SuccessStatus.OK, null));

        // when & then
        assertThatThrownBy(() -> rewardLedgerService.getRewardLedger(authenticatedUser, "EARN"))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode())
                            .isEqualTo(RewardErrorCode.INVALID_CORE_REWARD_RESPONSE);
                });
    }

    @Test
    @DisplayName("인증 사용자 정보가 없으면 예외가 발생한다")
    void getRewardLedgerAuthenticatedUserNull() {
        assertThatThrownBy(() -> rewardLedgerService.getRewardLedger(null, "EARN"))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode())
                            .isEqualTo(AuthErrorCode.AUTHENTICATION_REQUIRED);
                });
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                AUTH_USER_UUID,
                USER_UUID,
                "test-jti"
        );
    }

    private CardChnAuthUser newAuthUser(UUID userUuid, UUID cardUserUuid) {
        CardChnAuthUser user = CardChnAuthUser.builder()
                .authUserUuid(AUTH_USER_UUID)
                .userUuid(userUuid)
                .cardUserUuid(cardUserUuid)
                .build();

        return user;
    }

}
