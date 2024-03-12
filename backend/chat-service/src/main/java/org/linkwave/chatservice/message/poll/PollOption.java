package org.linkwave.chatservice.message.poll;

import java.util.List;

public record PollOption(String title, List<Long> votersIds) {
}
