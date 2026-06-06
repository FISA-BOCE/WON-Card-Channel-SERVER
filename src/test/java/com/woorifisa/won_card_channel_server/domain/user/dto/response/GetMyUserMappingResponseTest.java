package com.woorifisa.won_card_channel_server.domain.user.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GetMyUserMappingResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("common-server의 중첩 사용자 매핑 응답을 역직렬화한다")
    void deserializeNestedCommonServerResponse() throws Exception {
        String json = """
                {
                  "userUuid": "33333333-3333-3333-3333-333333333333",
                  "card": {
                    "cardUserUuid": "22222222-2222-2222-2222-222222222222",
                    "isConnected": true
                  },
                  "invest": {
                    "investUserUuid": "66666666-6666-6666-6666-666666666666",
                    "isConnected": true
                  }
                }
                """;

        GetMyUserMappingResponse response = objectMapper.readValue(json, GetMyUserMappingResponse.class);

        assertThat(response.userUuid()).isEqualTo(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        assertThat(response.card()).isNotNull();
        assertThat(response.card().cardUserUuid()).isEqualTo(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        assertThat(response.card().isConnected()).isTrue();
        assertThat(response.invest()).isNotNull();
        assertThat(response.invest().investUserUuid()).isEqualTo(UUID.fromString("66666666-6666-6666-6666-666666666666"));
        assertThat(response.invest().isConnected()).isTrue();
    }
}
