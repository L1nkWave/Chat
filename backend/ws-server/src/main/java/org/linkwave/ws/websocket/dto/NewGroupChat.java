package org.linkwave.ws.websocket.dto;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class NewGroupChat {

    private String name;
    private String description;
    private Boolean isPrivate;

}
