package com.woorifisa.won_card_channel_server.domain.card.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

// 프론트 요청 필드
public record CardApplicationCreateRequest(
        @NotNull @Positive Long cardProductId,
        @NotNull @Valid ApplicantInfo applicantInfo,
        @NotNull UUID investAccountUuid,
        @NotNull @Positive Long etfId,
        @NotBlank String ticker,
        @NotNull Boolean requiredTerms,
        @Valid OptionalTerms optionalTerms
) {

    public record ApplicantInfo(
            @NotBlank String nameKo,
            @NotBlank String nameEn,
            @NotBlank
            @Pattern(regexp = "^\\d{8}$", message = "birthDate는 yyyyMMdd 형식이어야 합니다.")
            String birthDate,
            @NotBlank String gender,
            @NotBlank String nationality,
            @NotBlank
            @Pattern(regexp = "^01\\d-?\\d{3,4}-?\\d{4}$", message = "phoneNumber 형식이 올바르지 않습니다.")
            String phoneNumber,
            @NotBlank
            @Email(message = "email 형식이 올바르지 않습니다.")
            String email,
            @NotBlank
            @Size(max = 255, message = "address는 255자를 초과할 수 없습니다.")
            String address,
            @NotBlank String job
    ) {
    }

    public record OptionalTerms(
            Boolean isMarketingEmailAgree,
            Boolean isMarketingSmsAgree
    ) {
    }
}
