package org.linkwave.chatservice.message;

import org.linkwave.chatservice.api.users.UserDto;
import org.modelmapper.ModelMapper;
import org.springframework.lang.NonNull;

import java.util.Map;

public interface FetchMessageMapping {

    MessageDto mapForFetch(@NonNull ModelMapper modelMapper, Long fetcherUserId, Map<Long, UserDto> users);

}
