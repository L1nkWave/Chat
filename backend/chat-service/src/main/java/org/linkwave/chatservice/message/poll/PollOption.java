package org.linkwave.chatservice.message.poll;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PollOption {

    private String title;
    private List<Long> votersIds;

}
