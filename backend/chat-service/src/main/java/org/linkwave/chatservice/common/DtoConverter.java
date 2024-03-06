package org.linkwave.chatservice.common;

import org.modelmapper.ModelMapper;
import org.springframework.lang.NonNull;

public interface DtoConverter<T> {
    T convert(@NonNull ModelMapper modelMapper);
}
