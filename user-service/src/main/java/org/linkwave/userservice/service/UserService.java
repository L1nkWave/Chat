package org.linkwave.userservice.service;

import org.linkwave.userservice.dto.UserDto;
import org.linkwave.userservice.dto.UserRegisterRequest;
import org.linkwave.userservice.entity.UserEntity;
import org.linkwave.userservice.security.DefaultUserDetails;
import org.springframework.data.util.Pair;

import java.util.List;

public interface UserService {
    UserEntity findById(Long userId);

    void register(UserRegisterRequest registerRequest);

    UserDto getUser(Long id);

    /**
     * @param userDetails details of the user that initiated request
     * @param username    match value
     * @param offset      how many records to skip
     * @param limit       how many record to return
     * @return pair the first value of is total count of matched users,
     * and the second - selected users with offset and limit params
     */
    Pair<Long, List<UserDto>> getUsersByUsername(
            DefaultUserDetails userDetails,
            String username,
            int offset, int limit
    );
}