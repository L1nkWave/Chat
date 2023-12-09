package org.linkwave.userservice.service;

import org.linkwave.userservice.dto.*;

public interface UserService {
    AuthDto register(UserRegisterRequest registerRequest);
    AuthDto login(UserLoginRequest loginRequest);
    AuthDto refresh(UserRefreshRequest refreshRequest);
    UserDto getPersonalInfo(Long userId);
}