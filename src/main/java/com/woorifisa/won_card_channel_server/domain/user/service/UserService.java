package com.woorifisa.won_card_channel_server.domain.user.service;

import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import com.woorifisa.won_card_channel_server.domain.user.dto.request.DeleteUserRequest;
import com.woorifisa.won_card_channel_server.domain.user.dto.response.GetMyUserResponse;
import com.woorifisa.won_card_channel_server.domain.user.dto.request.UpdateUserRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface UserService {

    GetMyUserResponse getMyUser(@NotNull AuthenticatedUser authenticatedUser);

    void withdrawUser(@NotNull AuthenticatedUser authenticatedUser, @NotNull @Valid DeleteUserRequest request);

    void updateUser(@NotNull AuthenticatedUser authenticatedUser, @NotNull @Valid UpdateUserRequest request);
}
