package org.linkwave.chatservice.api.users;

import lombok.*;

import java.time.ZonedDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@Builder
public class ContactDto {
    private UserDto user;
    private ZonedDateTime addedAt;
    private String alias;
}
