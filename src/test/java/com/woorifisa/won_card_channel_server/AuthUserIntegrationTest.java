package com.woorifisa.won_card_channel_server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisa.won_card_channel_server.domain.auth.model.CardChnAuthUser;
import com.woorifisa.won_card_channel_server.domain.auth.model.UserStatus;
import com.woorifisa.won_card_channel_server.domain.auth.repository.CardChnAuthSessionRepository;
import com.woorifisa.won_card_channel_server.domain.auth.repository.CardChnAuthUserRepository;
import com.woorifisa.won_card_channel_server.global.security.TextEncryptor;
import com.woorifisa.won_card_channel_server.global.util.HashUtils;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthUserIntegrationTest {

    private static final UUID AUTH_USER_UUID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_UUID_1 = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID AUTH_USER_UUID_2 = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID USER_UUID_2 = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CardChnAuthUserRepository userRepository;

    @Autowired
    private CardChnAuthSessionRepository sessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TextEncryptor textEncryptor;

    @BeforeEach
    void setUp() {
        sessionRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(CardChnAuthUser.builder()
                .authUserUuid(AUTH_USER_UUID_1)
                .userUuid(USER_UUID_1)
                .loginId(HashUtils.sha256("01012340214"))
                .userName("김우리")
                .emailEnc("encrypted@email")
                .telEnc(textEncryptor.encrypt("01012340214"))
                .telHash(HashUtils.sha256("01012340214"))
                .passwordHash(passwordEncoder.encode("password123!"))
                .userStatus(UserStatus.ACTIVE)
                .build());

        userRepository.save(CardChnAuthUser.builder()
                .authUserUuid(AUTH_USER_UUID_2)
                .userUuid(USER_UUID_2)
                .loginId(HashUtils.sha256("01012345678"))
                .userName("탈퇴회원")
                .telEnc(textEncryptor.encrypt("01012345678"))
                .telHash(HashUtils.sha256("01012345678"))
                .passwordHash(passwordEncoder.encode("password123!"))
                .userStatus(UserStatus.DEACTIVATE)
                .build());
    }

    @Test
    void loginSuccessStoresSession() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"01012340214","userPw":"password123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTH_200_002"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode body = objectMapper.readTree(response);
        assertThat(sessionRepository.findAll()).hasSize(1);
        assertThat(body.path("data").path("expiresIn").asLong()).isEqualTo(3600L);
    }

    @Test
    void signupSuccessCreatesUserWithoutSession() throws Exception {
        String response = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber":"01099998888",
                                  "userName":"신규회원",
                                  "password":"signup123!",
                                  "passwordConfirm":"signup123!",
                                  "email":"newuser@test.com",
                                  "termsAgreed":true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTH_200_001"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode data = objectMapper.readTree(response).path("data");
        assertThat(data.isNull()).isTrue();
        CardChnAuthUser savedUser = userRepository.findByTelHash(HashUtils.sha256("01099998888")).orElseThrow();
        assertThat(savedUser.getEmailEnc()).isNotEqualTo("newuser@test.com");
        assertThat(savedUser.getTelEnc()).isNotEqualTo("01099998888");
        assertThat(savedUser.getLoginId()).isEqualTo(HashUtils.sha256("01099998888"));
        assertThat(sessionRepository.findAll()).isEmpty();
    }

    @Test
    void signupFailsWhenPhoneNumberDuplicated() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber":"01012340214",
                                  "userName":"중복회원",
                                  "password":"signup123!",
                                  "passwordConfirm":"signup123!",
                                  "email":"dup@test.com",
                                  "termsAgreed":true
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_409_002"));
    }

    @Test
    void signupFailsWhenTermsNotAgreed() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber":"01055556666",
                                  "userName":"미동의회원",
                                  "password":"signup123!",
                                  "passwordConfirm":"signup123!",
                                  "email":"terms@test.com",
                                  "termsAgreed":false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_400_001"));
    }

    @Test
    void loginFailsWhenWithdrawnUser() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {"userId":"01012345678","userPw":"password123!"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403_002"));
    }

    @Test
    void reissueRotatesRefreshToken() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"01012340214","userPw":"password123!"}
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshToken = objectMapper.readTree(loginResponse).path("data").path("refreshToken").asText();

        String reissueResponse = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String rotatedRefreshToken = objectMapper.readTree(reissueResponse).path("data").path("refreshToken").asText();
        assertThat(rotatedRefreshToken).isNotEqualTo(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_004"));
    }

    @Test
    void logoutDeletesSession() throws Exception {
        TokenBundle tokenBundle = login();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenBundle.accessToken())
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(tokenBundle.refreshToken())))
                .andExpect(status().isOk());

        assertThat(sessionRepository.findAll()).isEmpty();
    }

    @Test
    void getMeMasksPhoneNumber() throws Exception {
        TokenBundle tokenBundle = login();

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenBundle.accessToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tel").value("010****0214"))
                .andExpect(jsonPath("$.data.userName").value("김우리"));
    }

    @Test
    void withdrawMarksUserAndDeletesSession() throws Exception {
        TokenBundle tokenBundle = login();

        mockMvc.perform(post("/api/users/me/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenBundle.accessToken())
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(tokenBundle.refreshToken())))
                .andExpect(status().isOk());

        CardChnAuthUser user = userRepository.findByUserUuid(USER_UUID_1).orElseThrow();
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.DEACTIVATE);
        assertThat(sessionRepository.findAll()).isEmpty();
    }

    @Test
    void updatePasswordChangesHash() throws Exception {
        TokenBundle tokenBundle = login();

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenBundle.accessToken())
                        .content("""
                                {"currentPw":"password123!","newPw":"newPassword123!"}
                                """))
                .andExpect(status().isOk());

        CardChnAuthUser updatedUser = userRepository.findByUserUuid(USER_UUID_1).orElseThrow();
        assertThat(passwordEncoder.matches("newPassword123!", updatedUser.getPasswordHash())).isTrue();
    }

    @Test
    void updateEmailStoresEncryptedValue() throws Exception {
        TokenBundle tokenBundle = login();

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenBundle.accessToken())
                        .content("""
                                {"email":"plain@mail.com"}
                                """))
                .andExpect(status().isOk());

        CardChnAuthUser updatedUser = userRepository.findByUserUuid(USER_UUID_1).orElseThrow();
        assertThat(updatedUser.getEmailEnc()).isNotEqualTo("plain@mail.com");
    }

    @Test
    void updateEmailRejectsInvalidEmailFormat() throws Exception {
        TokenBundle tokenBundle = login();

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenBundle.accessToken())
                        .content("""
                                {"email":"not-an-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_400_001"));
    }

    @Test
    void protectedApiRequiresAuthorization() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_002"));
    }

    private TokenBundle login() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"01012340214","userPw":"password123!"}
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode data = objectMapper.readTree(response).path("data");
        return new TokenBundle(data.path("accessToken").asText(), data.path("refreshToken").asText());
    }

    private record TokenBundle(String accessToken, String refreshToken) {
    }
}
