package com.woorifisa.won_card_channel_server.domain.reward.service;

import com.woorifisa.won_card_channel_server.domain.auth.model.CardChnAuthUser;
import com.woorifisa.won_card_channel_server.domain.auth.repository.CardChnAuthUserRepository;
import com.woorifisa.won_card_channel_server.domain.card.exception.CardErrorCode;
import com.woorifisa.won_card_channel_server.domain.reward.dto.response.CardCoreRewardLedgerResponse;
import com.woorifisa.won_card_channel_server.domain.reward.dto.response.RewardLedgerResponse;
import com.woorifisa.won_card_channel_server.domain.reward.exception.RewardErrorCode;
import com.woorifisa.won_card_channel_server.domain.reward.external.CardCoreRewardApi;
import com.woorifisa.won_card_channel_server.domain.reward.mapper.RewardLedgerMapper;
import com.woorifisa.won_card_channel_server.domain.reward.model.enums.RewardProcessStatus;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RewardLedgerService {

    private final CardChnAuthUserRepository cardChnAuthUserRepository;
    private final CardCoreRewardApi cardCoreRewardApi;
    private final RewardLedgerMapper rewardLedgerMapper;

    public RewardLedgerResponse getRewardLedger(AuthenticatedUser authenticatedUser, String type) {
        RewardProcessStatus rewardProcessStatus = RewardProcessStatus.from(type);

        UUID cardUserUuid = getCardUserUuid(authenticatedUser.userUuid());

        try {
            ApiResponse<CardCoreRewardLedgerResponse> coreResponse =
                    cardCoreRewardApi.getRewardLedger(cardUserUuid, rewardProcessStatus.name());

            return rewardLedgerMapper.toResponse(coreResponse.data());
        } catch (FeignException.NotFound e) {
            throw new BusinessException(RewardErrorCode.REWARD_LEDGER_NOT_FOUND, e);
        }
    }

    private UUID getCardUserUuid(UUID userUuid) {
        // 원앱 사용자이고 카드 고객은 아닐 수 있음
        CardChnAuthUser cardChnAuthUser = cardChnAuthUserRepository.findByUserUuid(userUuid)
                .orElseThrow(() -> new BusinessException(CardErrorCode.CARD_USER_NOT_FOUND));

        UUID cardUserUuid = cardChnAuthUser.getCardUserUuid();

        if (cardUserUuid == null) {
            throw new BusinessException(CardErrorCode.CARD_USER_NOT_FOUND);
        }

        return cardUserUuid;

    }
}
