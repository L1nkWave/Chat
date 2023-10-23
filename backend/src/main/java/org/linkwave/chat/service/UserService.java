package org.linkwave.chat.service;

import org.linkwave.chat.dto.*;

public interface UserService {
    AuthDto register(UserRegisterRequest registerRequest);
    AuthDto login(UserLoginRequest loginRequest);
    AuthDto refresh(UserRefreshRequest refreshRequest);
    UserDto getPersonalInfo(Long userId);
}