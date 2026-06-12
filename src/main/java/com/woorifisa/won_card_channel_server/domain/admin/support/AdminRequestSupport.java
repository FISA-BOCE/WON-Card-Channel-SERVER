package com.woorifisa.won_card_channel_server.domain.admin.support;

import com.woorifisa.won_card_channel_server.global.exception.code.CommonErrorCode;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class AdminRequestSupport {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    public static void validateCardSystemType(String systemType) {
        if (isAll(systemType) || AdminSystemType.CARD.equalsIgnoreCase(systemType)) {
            return;
        }

        throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
    }

    public static boolean isAll(String value) {
        return value == null || value.isBlank() || AdminSystemType.ALL.equalsIgnoreCase(value);
    }

    public static int normalizePage(int page) {
        return Math.max(page, 0);
    }

    public static int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }
}
