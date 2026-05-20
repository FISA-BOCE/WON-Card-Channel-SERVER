package com.woorifisa.won_card_channel_server.domain.auth.service;

import com.woorifisa.won_card_channel_server.domain.auth.dto.request.CreateLoginRequest;
import com.woorifisa.won_card_channel_server.domain.auth.dto.response.CreateLoginResponse;
import com.woorifisa.won_card_channel_server.domain.auth.dto.request.CreateTokenReissueRequest;
import com.woorifisa.won_card_channel_server.domain.auth.dto.response.CreateTokenReissueResponse;
import com.woorifisa.won_card_channel_server.domain.auth.dto.request.DeleteLogoutRequest;
import com.woorifisa.won_card_channel_server.domain.auth.dto.request.RegisterUserRequest;
import com.woorifisa.won_card_channel_server.domain.auth.dto.response.RegisterUserResponse;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface AuthService {

    RegisterUserResponse registerUser(@NotNull @Valid RegisterUserRequest request);

    CreateLoginResponse authenticateUser(@NotNull @Valid CreateLoginRequest request);

    CreateTokenReissueResponse reissueToken(@NotNull @Valid CreateTokenReissueRequest request);

    void logoutUser(AuthenticatedUser authenticatedUser, @NotNull @Valid DeleteLogoutRequest request);
}
