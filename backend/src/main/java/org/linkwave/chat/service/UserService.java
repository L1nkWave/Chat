package org.linkwave.chat.service;

import org.linkwave.chat.dto.AuthDto;
import org.linkwave.chat.dto.UserLoginRequest;
import org.linkwave.chat.dto.UserRefreshRequest;
import org.linkwave.chat.dto.UserRegisterRequest;

public interface UserService {
    AuthDto register(UserRegisterRequest registerRequest);
    AuthDto login(UserLoginRequest loginRequest);
    AuthDto refresh(UserRefreshRequest refreshRequest);
}