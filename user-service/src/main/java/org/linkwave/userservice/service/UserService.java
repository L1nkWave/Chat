package org.linkwave.userservice.service;

import org.linkwave.userservice.dto.*;

public interface UserService {
    void register(UserRegisterRequest registerRequest);
    UserDto getPersonalInfo(Long userId);
}