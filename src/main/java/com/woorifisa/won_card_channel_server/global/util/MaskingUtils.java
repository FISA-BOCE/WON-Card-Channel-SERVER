package com.woorifisa.won_card_channel_server.global.util;

public final class MaskingUtils {

    private MaskingUtils() {
    }

    public static String maskPhoneNumber(String tel) {
        if (ValidationUtils.isBlank(tel)) {
            return "***********";
        }
        String digits = tel.replaceAll("[^0-9]", "");
        if (digits.length() == 11) {
            return digits.substring(0, 3) + "****" + digits.substring(7);
        }
        if (digits.length() >= 7) {
            return digits.substring(0, 3) + "****" + digits.substring(digits.length() - 4);
        }
        return "****";
    }
}
