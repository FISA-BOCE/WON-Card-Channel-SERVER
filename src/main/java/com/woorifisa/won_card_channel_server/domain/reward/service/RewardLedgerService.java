package com.woorifisa.won_card_channel_server.domain.reward.service;

import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.domain.auth.model.CardChnAuthUser;
import com.woorifisa.won_card_channel_server.domain.auth.repository.CardChnAuthUserRepository;
import com.woorifisa.won_card_channel_server.domain.card.exception.code.CardErrorCode;
import com.woorifisa.won_card_channel_server.domain.reward.dto.response.CardCoreRewardLedgerDetailResponse;
import com.woorifisa.won_card_channel_server.domain.reward.dto.response.CardCoreRewardLedgerResponse;
import com.woorifisa.won_card_channel_server.domain.reward.dto.response.RewardLedgerDetailResponse;
import com.woorifisa.won_card_channel_server.domain.reward.dto.response.RewardLedgerResponse;
import com.woorifisa.won_card_channel_server.domain.reward.exception.code.RewardErrorCode;
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
        UUID userUuid = extractUserUuid(authenticatedUser);
        RewardProcessStatus rewardProcessStatus = RewardProcessStatus.from(type);
        UUID cardUserUuid = getCardUserUuid(userUuid);

        try {
            ApiResponse<CardCoreRewardLedgerResponse> coreResponse =
                    cardCoreRewardApi.getRewardLedger(cardUserUuid, rewardProcessStatus.name());

            CardCoreRewardLedgerResponse data = extractCoreRewardLedgerData(coreResponse);

            return rewardLedgerMapper.toResponse(data);
        } catch (FeignException e) {
            throw new BusinessException(RewardErrorCode.REWARD_INFORMATION_UNAVAILABLE, e);
        }
    }

    public RewardLedgerDetailResponse getRewardLedgerDetail(AuthenticatedUser authenticatedUser, Long pointLedgerId) {
        validatePointLedgerId(pointLedgerId);
        
        UUID userUuid = extractUserUuid(authenticatedUser);
        UUID cardUserUuid = getCardUserUuid(userUuid);

        try {
            ApiResponse<CardCoreRewardLedgerDetailResponse> coreResponse =
                    cardCoreRewardApi.getRewardLedgerDetail(cardUserUuid, pointLedgerId);

            CardCoreRewardLedgerDetailResponse data = extractRewardLedgerDetail(coreResponse);

            return rewardLedgerMapper.toDetailResponse(data);
        } catch (FeignException.NotFound e) {
            throw new BusinessException(RewardErrorCode.REWARD_LEDGER_NOT_FOUND, e);
        } catch (FeignException.Forbidden e) {
            throw new BusinessException(RewardErrorCode.REWARD_LEDGER_FORBIDDEN, e);
        } catch (FeignException e) {
            throw new BusinessException(RewardErrorCode.REWARD_INFORMATION_UNAVAILABLE, e);
        }
    }

    private void validatePointLedgerId(Long pointLedgerId) {
        if (pointLedgerId == null || pointLedgerId <= 0) {
            throw new BusinessException(RewardErrorCode.INVALID_REWARD_LEDGER_ID);
        }
    }

    private CardCoreRewardLedgerDetailResponse extractRewardLedgerDetail(ApiResponse<CardCoreRewardLedgerDetailResponse> coreResponse) {
        if (coreResponse == null || coreResponse.data() == null || coreResponse.data().detail() == null) {
            throw new BusinessException(RewardErrorCode.INVALID_REWARD_RESPONSE);
        }

        return coreResponse.data();
    }

    private UUID extractUserUuid(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.userUuid() == null) {
            throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
        }

        return authenticatedUser.userUuid();
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

    private CardCoreRewardLedgerResponse extractCoreRewardLedgerData(
            ApiResponse<CardCoreRewardLedgerResponse> coreResponse) {

        if (coreResponse == null || coreResponse.data() == null) {
            throw new BusinessException(RewardErrorCode.INVALID_REWARD_RESPONSE);
        }

        return coreResponse.data();
    }
}
