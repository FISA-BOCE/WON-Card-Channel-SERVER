package com.woorifisa.won_card_channel_server.domain.user.api;

import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import com.woorifisa.won_card_channel_server.domain.user.dto.request.DeleteUserRequest;
import com.woorifisa.won_card_channel_server.domain.user.dto.response.GetMyUserResponse;
import com.woorifisa.won_card_channel_server.domain.user.dto.request.UpdateUserRequest;
import com.woorifisa.won_card_channel_server.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserApi {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<GetMyUserResponse>> getMyUser(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity
                .status(SuccessStatus.USER_ME_SUCCESS.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.USER_ME_SUCCESS, userService.getMyUser(authenticatedUser)));
    }

    @PostMapping("/me/withdraw")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody DeleteUserRequest request
    ) {
        userService.withdrawUser(authenticatedUser, request);
        return ResponseEntity
                .status(SuccessStatus.USER_WITHDRAW_SUCCESS.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.USER_WITHDRAW_SUCCESS));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateUser(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        userService.updateUser(authenticatedUser, request);
        return ResponseEntity
                .status(SuccessStatus.USER_UPDATE_SUCCESS.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.USER_UPDATE_SUCCESS));
    }
}
