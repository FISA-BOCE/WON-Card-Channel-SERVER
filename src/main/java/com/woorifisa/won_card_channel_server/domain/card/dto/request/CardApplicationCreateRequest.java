package com.woorifisa.won_card_channel_server.domain.card.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

// 프론트 요청 필드
public record CardApplicationCreateRequest(
        @NotNull @Positive Long cardProductId,
        @NotNull @Valid ApplicantInfo applicantInfo,
        @NotNull UUID invstAccountUuid,
        @NotNull @Positive Long etfId,
        @NotBlank String ticker,
        @NotNull Boolean requiredTerms,
        @Valid OptionalTerms optionalTerms
) {

    public record ApplicantInfo(
            @NotBlank String nameKo,
            @NotBlank String nameEn,
            @NotBlank String birthDateEnc,
            @NotBlank String gender,
            @NotBlank String nationality,
            @NotBlank String telEnc,
            @NotBlank String emailEnc,
            @NotBlank String addressEnc,
            @NotBlank String job
    ) {
    }

    public record OptionalTerms(
            Boolean isMarketingEmailAgree,
            Boolean isMarketingSmsAgree
    ) {
    }
}
