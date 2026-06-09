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

    public static CardCoreApplicationRequest from(
            CardApplicationCreateRequest request,
            String userNameEnc,
            String birthDateEnc,
            String telEnc,
            String emailEnc,
            String addressEnc
    ) {
        return new CardCoreApplicationRequest(
                userNameEnc,
                birthDateEnc,
                request.applicantInfo().gender(),
                request.applicantInfo().nationality(),
                request.requiredTerms(),
                telEnc,
                emailEnc,
                addressEnc
        );
    }
}
