package com.woorifisa.won_card_channel_server.global.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SuccessStatus {

    OK(HttpStatus.OK, "COM_200_001", "OK"),
    CREATED(HttpStatus.CREATED, "COM_201_001", "CREATED"),
    NO_CONTENT(HttpStatus.NO_CONTENT, "COM_204_001", "NO_CONTENT"),

    SIGNUP_SUCCESS(HttpStatus.OK, "AUTH_200_001", "회원가입이 완료되었습니다."),
    LOGIN_SUCCESS(HttpStatus.OK, "AUTH_200_002", "로그인이 완료되었습니다."),
    TOKEN_REISSUE_SUCCESS(HttpStatus.OK, "AUTH_200_003", "토큰 재발급이 완료되었습니다."),
    LOGOUT_SUCCESS(HttpStatus.OK, "AUTH_200_004", "로그아웃이 완료되었습니다."),

    USER_ME_SUCCESS(HttpStatus.OK, "USER_200_001", "회원 정보 조회가 완료되었습니다."),
    USER_WITHDRAW_SUCCESS(HttpStatus.OK, "USER_200_002", "회원 탈퇴가 완료되었습니다."),
    USER_UPDATE_SUCCESS(HttpStatus.OK, "USER_200_003", "회원 정보 수정이 완료되었습니다."),

    REWARD_LEDGER_FOUND(HttpStatus.OK, "REWARD_200_001", "리워드 내역 조회가 완료되었습니다."),
    REWARD_LEDGER_DETAIL_FOUND(HttpStatus.OK, "REWARD_200_002", "상세 리워드 내역 조회가 완료되었습니다."),

    SWEEP_REQUEST_CREATED(HttpStatus.CREATED, "SWEEP_201_001", "스윕 요청이 생성되었습니다."),
    SWEEP_REQUEST_BATCH_CREATED(HttpStatus.CREATED, "SWEEP_201_002", "자동 스윕 요청 배치가 완료되었습니다."),

    
    // 자동투자
    AUTO_INVEST_SUBSCRIPTION_CREATED(HttpStatus.CREATED, "AUTO_201_001", "자동투자 신청이 완료되었습니다."),
    AUTO_INVEST_SUBSCRIPTION_FOUND(HttpStatus.OK, "AUTO_200_001", "자동투자 설정 조회가 완료되었습니다."),
    AUTO_INVEST_SUBSCRIPTION_CHANGED(HttpStatus.OK, "AUTO_200_002", "ETF 변경이 완료되었습니다."),

    // 카드
    CARD_APPLICATION_CREATED(HttpStatus.CREATED, "CARD_201_001", "카드 신청이 완료되었습니다."),
    CARD_SUMMARY_FOUND(HttpStatus.OK, "CARD_200_001", "카드 정보 조회가 완료되었습니다."),
    CARD_SUMMARY_NOT_FOUND(HttpStatus.OK, "CARD_200_002", "신청된 카드 정보가 없습니다."),
    CARD_INVEST_ACCOUNTS_FOUND(HttpStatus.OK, "CARD_200_003", "증권 계좌 목록 조회가 완료되었습니다."),
    PREVIOUS_PERFORMANCE_FOUND(HttpStatus.OK, "CARD_200_004", "전월 실적 조회가 완료되었습니다."),

    CURRENT_SPEND_AMOUNT_FOUND(HttpStatus.OK, "SPEND_200_001", "당월 이용 금액 조회가 완료되었습니다."),

    // AI 챗봇
    CHAT_SUCCESS(HttpStatus.OK, "AI_200_001", "답변이 생성되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    SuccessStatus(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
