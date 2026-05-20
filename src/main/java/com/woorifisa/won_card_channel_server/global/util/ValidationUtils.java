package com.woorifisa.won_card_channel_server.global.util;

import java.util.regex.Pattern;

public final class ValidationUtils {

    private static final Pattern USER_ID_PATTERN = Pattern.compile("^01[0-9][0-9]{7,8}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private ValidationUtils() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isValidUserId(String value) {
        return value != null && USER_ID_PATTERN.matcher(value).matches();
    }

    public static boolean isValidPassword(String value) {
        if (isBlank(value) || value.length() < 8) {
            return false;
        }
        boolean hasLetter = value.chars().anyMatch(Character::isLetter);
        boolean hasDigit = value.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = value.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch));
        int categories = (hasLetter ? 1 : 0) + (hasDigit ? 1 : 0) + (hasSpecial ? 1 : 0);
        return categories >= 2;
    }

    public static boolean isValidEmail(String value) {
        return value != null && value.length() <= 255 && EMAIL_PATTERN.matcher(value).matches();
    }

    public static String maskPhoneNumber(String tel) {
        if (isBlank(tel)) {
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
