package org.linkwave.chatservice.chat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChatMember {

    private Long id;
    private ChatRole role;
    private Instant joinedAt;

    /** Người này đã ẩn đoạn chat tại thời điểm này: chỉ thấy tin gửi sau mốc này. Null nghĩa là không ẩn. */
    private Instant hiddenBefore;

    /** Người này đã ghim đoạn chat (tối đa 5 đoạn cho mỗi người). Không trả ra JSON: danh sách thành viên là công khai. */
    @JsonIgnore
    private boolean pinned;

    /** Thời điểm ghim. Null khi chưa ghim. Không trả ra JSON. */
    @JsonIgnore
    private Instant pinnedAt;

    public ChatMember(Long id, ChatRole role, Instant joinedAt) {
        this(id, role, joinedAt, null, false, null);
    }

}
