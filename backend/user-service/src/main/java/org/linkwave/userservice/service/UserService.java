package org.linkwave.userservice.service;

import org.linkwave.userservice.dto.UserDto;
import org.linkwave.userservice.dto.UserRegisterRequest;
import org.linkwave.userservice.entity.UserEntity;
import org.linkwave.shared.auth.DefaultUserDetails;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    UserEntity findById(Long userId);

    void register(UserRegisterRequest registerRequest);

    UserDto getUser(Long id);

    /**
     * Performs users global searching and ignores users which initiator is familiar with.
     *
     * @param userDetails details of the user that initiated request
     * @param username    match value
     * @param offset      how many records to skip
     * @param limit       how many record to return
     * @return pair the first value of is total count of matched users,
     * and the second - selected users with offset and limit params
     */
    Pair<Long, List<UserDto>> getUsersByUsernameWithoutContacts(
            DefaultUserDetails userDetails,
            String username,
            int offset, int limit
    );

    /**
     * Finds and returns a list of users by a given list of ids.
     *
     * @param usersIds any users ids
     * @return list of users dto representation
     */
    List<UserDto> getUsers(@NonNull List<Long> usersIds);

    /**
     * Sets passed avatar to specific user.
     *
     * @param userId any user ID
     * @param avatar image
     */
    void changeUserAvatar(Long userId, @NonNull MultipartFile avatar);

    /**
     * Retrieve user avatar as array of bytes.
     *
     * @param userId any user ID
     * @return bytes that represent the avatar
     */
    byte[] getUserAvatar(Long userId);

    /**
     * Removes the avatar for specified user.
     *
     * @param userId any user ID
     */
    void deleteUserAvatar(Long userId);

}