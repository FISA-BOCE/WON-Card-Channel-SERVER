package com.woorifisa.won_card_channel_server.domain.user.api;

import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import com.woorifisa.won_card_channel_server.domain.user.dto.request.DeleteUserRequest;
import com.woorifisa.won_card_channel_server.domain.user.dto.response.GetMyUserResponse;
import com.woorifisa.won_card_channel_server.domain.user.dto.request.UpdateUserRequest;
import com.woorifisa.won_card_channel_server.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "User", description = "앱 유저 관련 API")
public class UserApi {

    private final UserService userService;

    @Operation(summary = "내 정보 조회", description = "마이페이지 내 정보 조회를 위한 API입니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<GetMyUserResponse>> getMyUser(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "트랜잭션 추적용 ID")
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId
    ) {
        return ResponseEntity
                .status(SuccessStatus.USER_ME_SUCCESS.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.USER_ME_SUCCESS, userService.getMyUser(authenticatedUser)));
    }

    @Operation(summary = "회원 탈퇴", description = "회원 탈퇴 API입니다.")
    @PostMapping("/me/withdraw")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "트랜잭션 추적용 ID")
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId,
            @Valid @RequestBody DeleteUserRequest request
    ) {
        userService.withdrawUser(authenticatedUser, request);
        return ResponseEntity
                .status(SuccessStatus.USER_WITHDRAW_SUCCESS.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.USER_WITHDRAW_SUCCESS));
    }

    @Operation(summary = "내 정보 수정", description = "마이페이지 내 정보 수정을 위한 API입니다.")
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateUser(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "트랜잭션 추적용 ID")
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        userService.updateUser(authenticatedUser, request);
        return ResponseEntity
                .status(SuccessStatus.USER_UPDATE_SUCCESS.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.USER_UPDATE_SUCCESS));
    }
}
