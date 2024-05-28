import { GroupMemberDetails } from "@/api/http/contacts/contacts.types";

export enum MessageAction {
  ONLINE = "ONLINE",
  OFFLINE = "OFFLINE",
  BIND = "BIND",
  MESSAGE = "MESSAGE",
  UNREAD_MESSAGES = "UNREAD_MESSAGES",
  READ = "READ",
  ADD = "ADD",
  FILE = "FILE",
}

export type MessageActionParams = {
  action: MessageAction;
};

export type OnlineOfflineMessage = {
  senderId: string;
  timestamp: string;
} & MessageActionParams;

export type BindMessage = {
  chatId: string;
  tmpMessageId: string;
  messageId: string;
} & MessageActionParams;

export type MessageLikeMessage = {
  timestamp: number;
  id: string;
  chatId: string;
  senderId: string;
  text: string;
  filename: string;
  contentType: string;
  size: number;
} & MessageActionParams;

export type UnreadMessagesMessage = {
  timestamp: number;
  chats: Record<string, number>;
} & MessageActionParams;

export type ReadMessage = {
  timestamp: number;
  senderId: number;
  chatId: string;
  messages: string[];
} & MessageActionParams;

export type AddMessage = {
  timestamp: number;
  senderId: string;
  chatId: string;
  memberId: string;
  memberDetails: GroupMemberDetails;
} & MessageActionParams;

export type SocketMessageType =
  | OnlineOfflineMessage
  | BindMessage
  | MessageLikeMessage
  | UnreadMessagesMessage
  | ReadMessage
  | AddMessage;

export type SocketContextProps = {
  webSocket?: WebSocket | null;
  socketMessage?: SocketMessageType;
};
