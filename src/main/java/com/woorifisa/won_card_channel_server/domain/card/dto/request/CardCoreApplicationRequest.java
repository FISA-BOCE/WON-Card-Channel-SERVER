package com.woorifisa.won_card_channel_server.domain.card.dto.request;

public record CardCoreApplicationRequest(
        String userNameEnc,
        String birthDateEnc,
        String gender,
        String nationality,
        Boolean isAgree,
        String telEnc,
        String emailEnc,
        String addressEnc
) {

    public static CardCoreApplicationRequest from(CardApplicationCreateRequest request, String userNameEnc) {
        return new CardCoreApplicationRequest(
                userNameEnc,
                request.applicantInfo().birthDateEnc(),
                request.applicantInfo().gender(),
                request.applicantInfo().nationality(),
                request.requiredTerms(),
                request.applicantInfo().telEnc(),
                request.applicantInfo().emailEnc(),
                request.applicantInfo().addressEnc()
        );
    }
}
