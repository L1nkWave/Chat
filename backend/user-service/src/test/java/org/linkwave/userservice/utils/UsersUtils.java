package org.linkwave.userservice.utils;

import lombok.experimental.UtilityClass;
import org.linkwave.userservice.entity.RoleEntity;
import org.linkwave.userservice.entity.UserEntity;

import java.util.List;
import java.util.stream.IntStream;

import static java.lang.String.format;

@UtilityClass
public class UsersUtils {

    public static List<UserEntity> generateUsers(final int count,
                                                 final String username,
                                                 final RoleEntity role) {

        final List<RoleEntity> roles = List.of(role);
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> {
                    final var _username = format("%s%d", username, i);
                    final var _name = format("User%d", i);

                    final var user = UserEntity.builder()
                            .id((long) i)
                            .name(_name)
                            .username(_username)
                            .password("1132678dsd")
                            .roles(roles)
                            .build();

                    role.getUsers().add(user);
                    return user;
                })
                .toList();
    }

}
