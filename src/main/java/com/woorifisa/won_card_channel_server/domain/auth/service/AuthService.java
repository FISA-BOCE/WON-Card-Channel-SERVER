package com.woorifisa.won_card_channel_server.domain.auth.service;

import com.woorifisa.won_card_channel_server.domain.auth.dto.CreateLoginRequest;
import com.woorifisa.won_card_channel_server.domain.auth.dto.CreateLoginResponse;
import com.woorifisa.won_card_channel_server.domain.auth.dto.CreateTokenReissueRequest;
import com.woorifisa.won_card_channel_server.domain.auth.dto.CreateTokenReissueResponse;
import com.woorifisa.won_card_channel_server.domain.auth.dto.DeleteLogoutRequest;
import com.woorifisa.won_card_channel_server.domain.auth.dto.RegisterUserRequest;
import com.woorifisa.won_card_channel_server.domain.auth.dto.RegisterUserResponse;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;

public interface AuthService {

    RegisterUserResponse registerUser(RegisterUserRequest request);

    CreateLoginResponse authenticateUser(CreateLoginRequest request);

    CreateTokenReissueResponse reissueToken(CreateTokenReissueRequest request);

    void logoutUser(AuthenticatedUser authenticatedUser, DeleteLogoutRequest request);
}
