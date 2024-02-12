package org.linkwave.userservice.dto;

import lombok.*;

import java.time.ZonedDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ContactDto {
    private UserDto user;
    private ZonedDateTime addedAt;
    private String alias;
}
