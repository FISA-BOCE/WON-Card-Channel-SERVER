package com.woorifisa.won_card_channel_server.global.util;

import java.util.regex.Pattern;

public final class PiiTextMasker {

    // 주민등록번호: 901231-1234567
    private static final Pattern RESIDENT_REG_NO =
            Pattern.compile("\\d{6}-[1-4]\\d{6}");

    // 카드번호: 1234-5678-9012-3456 or 1234 5678 9012 3456 (구분자 필수 — 연속 16자리 오탐 방지)
    private static final Pattern CARD_NO =
            Pattern.compile("\\d{4}[- ]\\d{4}[- ]\\d{4}[- ]\\d{4}");

    // 전화번호: 010-1234-5678 / 01012345678 / 010.1234.5678
    private static final Pattern PHONE_NO =
            Pattern.compile("(01[016789])[-.\\s]?(\\d{3,4})[-.\\s]?(\\d{4})");

    private PiiTextMasker() {}

    public static String mask(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String result = RESIDENT_REG_NO.matcher(text).replaceAll("******-*******");
        result = CARD_NO.matcher(result).replaceAll("****-****-****-****");
        result = PHONE_NO.matcher(result).replaceAll("$1-****-****");
        return result;
    }
}
