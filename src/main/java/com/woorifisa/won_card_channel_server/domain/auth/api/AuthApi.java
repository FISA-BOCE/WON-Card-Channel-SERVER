package com.woorifisa.won_card_channel_server.domain.auth.api;

import com.woorifisa.won_card_channel_server.domain.auth.dto.request.CreateLoginRequest;
import com.woorifisa.won_card_channel_server.domain.auth.dto.response.CreateLoginResponse;
import com.woorifisa.won_card_channel_server.domain.auth.dto.request.CreateTokenReissueRequest;
import com.woorifisa.won_card_channel_server.domain.auth.dto.response.CreateTokenReissueResponse;
import com.woorifisa.won_card_channel_server.domain.auth.dto.request.DeleteLogoutRequest;
import com.woorifisa.won_card_channel_server.domain.auth.dto.request.RegisterUserRequest;
import com.woorifisa.won_card_channel_server.domain.auth.dto.response.RegisterUserResponse;
import com.woorifisa.won_card_channel_server.domain.auth.service.AuthService;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "인증 관련 API")
public class AuthApi {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "회원가입 API입니다.    \n비밀번호는 8자 이상이어야 합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<RegisterUserResponse>> createUserRegistration(@Valid @RequestBody RegisterUserRequest request) {
        return ResponseEntity
                .status(SuccessStatus.SIGNUP_SUCCESS.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.SIGNUP_SUCCESS, authService.registerUser(request)));
    }

    @Operation(summary = "로그인", description = "로그인 API입니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<CreateLoginResponse>> createUserLogin(@Valid @RequestBody CreateLoginRequest request) {
        return ResponseEntity
                .status(SuccessStatus.LOGIN_SUCCESS.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.LOGIN_SUCCESS, authService.authenticateUser(request)));
    }

    @Operation(summary = "토큰 재발급", description = "토큰 재발급 API입니다.    \nRefresh Token을 통해 Access Token을 재발급합니다.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<CreateTokenReissueResponse>> createTokenReissue(@Valid @RequestBody CreateTokenReissueRequest request) {
        return ResponseEntity
                .status(SuccessStatus.TOKEN_REISSUE_SUCCESS.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.TOKEN_REISSUE_SUCCESS, authService.reissueToken(request)));
    }

    @Operation(summary = "로그아웃", description = "로그아웃 API입니다. session을 삭제합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> deleteUserLogout(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody DeleteLogoutRequest request
    ) {
        authService.logoutUser(authenticatedUser, request);
        return ResponseEntity
                .status(SuccessStatus.LOGOUT_SUCCESS.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.LOGOUT_SUCCESS));
    }
}
