package org.linkwave.chatservice.chat.group;

/** Loại nhóm chat. Đoạn chat riêng (DUO) không có loại, DTO trả về "PRIVATE". */
public enum ChatCategory {
    GROUP,
    CLASS,
    SUBJECT,
    FACULTY
}
