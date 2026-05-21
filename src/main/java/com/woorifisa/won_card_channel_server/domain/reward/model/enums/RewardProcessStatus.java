package com.woorifisa.won_card_channel_server.domain.reward.model.enums;

import com.woorifisa.won_card_channel_server.domain.reward.exception.code.RewardErrorCode;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;

public enum RewardProcessStatus {
    ALL, EARN, NOT_APPLIED, HOLD;

    public static RewardProcessStatus from(String type) {
        if (type == null || type.isBlank()) {
            return ALL;
        }

        try {
            return RewardProcessStatus.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(RewardErrorCode.INVALID_REWARD_LEDGER_TYPE, e);
        }
    }
}
