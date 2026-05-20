package com.woorifisa.won_card_channel_server.domain.reward.service;

import com.woorifisa.won_card_channel_server.domain.reward.dto.response.CardCoreRewardLedgerResponse;
import com.woorifisa.won_card_channel_server.domain.reward.dto.response.RewardLedgerResponse;
import com.woorifisa.won_card_channel_server.domain.reward.exception.RewardErrorCode;
import com.woorifisa.won_card_channel_server.domain.reward.external.CardCoreRewardApi;
import com.woorifisa.won_card_channel_server.domain.reward.mapper.RewardLedgerMapper;
import com.woorifisa.won_card_channel_server.domain.reward.model.enums.RewardProcessStatus;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RewardLedgerService {

    private final CardCoreRewardApi cardCoreRewardApi;
    private final RewardLedgerMapper rewardLedgerMapper;

    public RewardLedgerResponse getRewardLedger(UUID cardUserUuid, String type) {
        RewardProcessStatus rewardProcessStatus = RewardProcessStatus.from(type);

        try {
            ApiResponse<CardCoreRewardLedgerResponse> coreResponse =
                    cardCoreRewardApi.getRewardLedger(cardUserUuid, rewardProcessStatus.name());

            return rewardLedgerMapper.toResponse(coreResponse.data());
        } catch (FeignException.NotFound e) {
            throw new BusinessException(RewardErrorCode.REWARD_LEDGER_NOT_FOUND);
        }
    }
}
