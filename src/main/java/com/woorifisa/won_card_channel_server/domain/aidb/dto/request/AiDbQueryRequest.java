package com.woorifisa.won_card_channel_server.domain.aidb.dto.request;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record AiDbQueryRequest(
        @NotNull
        UUID userUuid,

        @NotBlank
        String queryType,

        @NotNull
        @Valid
        Params params
) {

    public record Params(
            @NotBlank
            @Pattern(regexp = "^\\d{4}-\\d{2}$")
            String baseMonth
    ) {
    }
}
