import React from "react";

import { MessageParams } from "@/api/http/contacts/contacts.types";
import { ChatBoxProps } from "@/components/Chat/MainBox/variants/ChatBox/chatBox.types";
import { ChatHeader } from "@/components/Chat/MainBox/variants/ChatBox/ChatHeader/ChatHeader";
import { MessageContainer } from "@/components/Chat/MainBox/variants/ChatBox/MessageBox/MessageContainer/MessageContainer";
import { MessageInput } from "@/components/Chat/MainBox/variants/ChatBox/MessageInput/MessageInput";

export function ChatBox({ contact }: Readonly<ChatBoxProps>) {
  const messages: MessageParams[] = [
    {
      id: "1",
      action: "message",
      reactions: [],
      createdAt: "2412532412412",
      edited: false,
      isRead: true,
      text: "We want to encourage a robust community for the Spring open source project.",
      author: { id: 1, online: true, username: "bot", name: "Bot", lastSeen: 0, createdAt: "", avatarPath: "" },
    },
    {
      id: "2",
      action: "message",
      reactions: [],
      createdAt: "",
      edited: false,
      isRead: true,
      text: "Therefore, you may do any of the following, as long as you do so in a way that does not devalue. In other words, when you do these things, you should behave responsibly and reasonably in the interest of the community, but you do not need a trademark license from us to do them.",
      author: { id: 1, online: true, username: "bot", name: "Bot", lastSeen: 0, createdAt: "", avatarPath: "" },
    },
    {
      id: "3",
      action: "message",
      reactions: [],
      createdAt: "124125125324",
      edited: false,
      isRead: true,
      text: "Yeap. You may engage in “nominative use” of the Spring name, but this does not allow you to use the logo.",
      author: { id: 2, online: true, username: "bot", name: "Bot", lastSeen: 0, createdAt: "", avatarPath: "" },
    },
    {
      id: "4",
      action: "message",
      reactions: [],
      createdAt: "124151234125124",
      edited: false,
      isRead: true,
      text: "You may use the Spring Marks in connection with development of tools, add-ons, etc.",
      author: { id: 1, online: true, username: "bot", name: "Bot", lastSeen: 0, createdAt: "", avatarPath: "" },
    },
    {
      id: "5",
      action: "message",
      reactions: [],
      createdAt: "124151234125124",
      edited: false,
      isRead: true,
      text: "You may use the Spring Marks in connection with development of tools, add-ons, etc.",
      author: { id: 1, online: true, username: "bot", name: "Bot", lastSeen: 0, createdAt: "", avatarPath: "" },
    },
    {
      id: "6",
      action: "message",
      reactions: [],
      createdAt: "124151234125124",
      edited: false,
      isRead: true,
      text: "You may use the Spring Marks in connection with development of tools, add-ons, etc.",
      author: { id: 1, online: true, username: "bot", name: "Bot", lastSeen: 0, createdAt: "", avatarPath: "" },
    },
    {
      id: "7",
      action: "message",
      reactions: [],
      createdAt: "124151234125124",
      edited: false,
      isRead: true,
      text: "You may use the Spring Marks in connection with development of tools, add-ons, etc.",
      author: { id: 1, online: true, username: "bot", name: "Bot", lastSeen: 0, createdAt: "", avatarPath: "" },
    },
    {
      id: "8",
      action: "message",
      reactions: [],
      createdAt: "124151234125124",
      edited: false,
      isRead: true,
      text: "You may use the Spring Marks in connection with development of tools, add-ons, etc.",
      author: { id: 1, online: true, username: "bot", name: "Bot", lastSeen: 0, createdAt: "", avatarPath: "" },
    },
    {
      id: "9",
      action: "message",
      reactions: [],
      createdAt: "124151234125124",
      edited: false,
      isRead: true,
      text: "You may use the Spring Marks in connection with development of tools, add-ons, etc.",
      author: { id: 1, online: true, username: "bot", name: "Bot", lastSeen: 0, createdAt: "", avatarPath: "" },
    },
    {
      id: "10",
      action: "message",
      reactions: [],
      createdAt: "124151234125124",
      edited: false,
      isRead: true,
      text: "You may use the Spring Marks in connection with development of tools, add-ons, etc.",
      author: { id: 1, online: true, username: "bot", name: "Bot", lastSeen: 0, createdAt: "", avatarPath: "" },
    },
    {
      id: "11",
      action: "message",
      reactions: [],
      createdAt: "124151234125124",
      edited: false,
      isRead: true,
      text: "You may use the Spring Marks in connection with development of tools, add-ons, etc.",
      author: { id: 1, online: true, username: "bot", name: "Bot", lastSeen: 0, createdAt: "", avatarPath: "" },
    },
    {
      id: "12",
      action: "message",
      reactions: [],
      createdAt: "124151234125124",
      edited: false,
      isRead: true,
      text: "You may use the Spring Marks in connection with development of tools, add-ons, etc.",
      author: { id: 1, online: true, username: "bot", name: "Bot", lastSeen: 0, createdAt: "", avatarPath: "" },
    },
    {
      id: "13",
      action: "message",
      reactions: [],
      createdAt: "124151234125124",
      edited: false,
      isRead: true,
      text: "You may use the Spring Marks in connection with development of tools, add-ons, etc.",
      author: { id: 1, online: true, username: "bot", name: "Bot", lastSeen: 0, createdAt: "", avatarPath: "" },
    },
    {
      id: "14",
      action: "message",
      reactions: [],
      createdAt: "124151234125124",
      edited: false,
      isRead: true,
      text: "You may use the Spring Marks in connection with development of tools, add-ons, etc.",
      author: { id: 1, online: true, username: "bot", name: "Bot", lastSeen: 0, createdAt: "", avatarPath: "" },
    },
    {
      id: "15",
      action: "message",
      reactions: [],
      createdAt: "124151234125124",
      edited: false,
      isRead: true,
      text: "You may use the Spring Marks in connection with development of tools, add-ons, etc.",
      author: { id: 1, online: true, username: "bot", name: "Bot", lastSeen: 0, createdAt: "", avatarPath: "" },
    },
  ];
  return (
    <div className="relative h-full w-full flex flex-col justify-between">
      <ChatHeader user={contact.user} />
      <MessageContainer messages={messages} />
      <MessageInput />
    </div>
  );
}
