package com.woorifisa.won_card_channel_server.domain.autoinvest.exception.code;

import com.woorifisa.won_card_channel_server.global.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AutoInvestErrorCode implements ErrorCode {

    // 자동투자 ETF
    AUTO_INVEST_SAME_ETF(HttpStatus.BAD_REQUEST, "AUTO_400_001", "현재 적립 중인 ETF와 동일한 ETF입니다."),
    AUTO_INVEST_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "AUTO_400_002", "자동투자 신청 값이 올바르지 않습니다."),
    AUTO_INVEST_INITIAL_REQUEST_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "AUTO_400_003", "최초 자동투자 설정은 카드 신청 시에만 생성할 수 있습니다."),

    AUTO_INVEST_NOT_OWNER(HttpStatus.FORBIDDEN, "AUTO_403_001", "본인의 자동투자 설정이 아닙니다."),

    AUTO_INVEST_CHANGE_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTO_404_001", "자동투자 설정 정보를 찾을 수 없습니다."),
    AUTO_INVEST_RESUME_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTO_404_002", "재개할 자동투자 이력이 없습니다."),

    AUTO_INVEST_ALREADY_ACTIVE(HttpStatus.CONFLICT, "AUTO_409_001", "이미 활성화된 자동투자 설정이 존재합니다."),
    AUTO_INVEST_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTO_500_001", "자동투자 설정 저장 중 오류가 발생했습니다."),

    // 매핑 관련
    INVEST_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "INVST_404_001", "증권계좌를 찾을 수 없습니다."),

    INVEST_ACCOUNT_FORBIDDEN(HttpStatus.FORBIDDEN, "INVST_403_001", "타인의 증권계좌에는 접근할 수 없습니다."),

    INVEST_ACCOUNT_INVALID_STATUS(HttpStatus.BAD_REQUEST, "INVST_400_001", "사용할 수 없는 증권계좌 상태입니다."),
    INVEST_ACCOUNT_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "INVST_502_001", "증권계좌 검증 응답 형식이 올바르지 않습니다."),
    INVEST_ACCOUNT_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "INVST_502_002", "증권계좌 검증 연동에 실패했습니다."),

    // ETF 목록
    ETF_NOT_FOUND(HttpStatus.NOT_FOUND, "ETF_404_001", "ETF 상품을 찾을 수 없습니다."),

    ETF_NOT_TRADABLE(HttpStatus.UNPROCESSABLE_ENTITY, "ETF_422_001", "자동투자에 사용할 수 없는 ETF 상품입니다."),

    ETF_FRACTIONAL_BUY_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "ETF_400_002", "소수점 매수가 불가능한 ETF 상품입니다."),
    ETF_TICKER_MISMATCH(HttpStatus.BAD_REQUEST, "ETF_400_003", "ETF ID와 ticker가 일치하지 않습니다."),

    ETF_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "ETF_502_001", "ETF 검증 응답 형식이 올바르지 않습니다."),
    ETF_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "ETF_502_002", "ETF 검증 연동에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    AutoInvestErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
