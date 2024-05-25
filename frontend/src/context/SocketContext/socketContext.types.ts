export enum MessageAction {
  ONLINE = "ONLINE",
  OFFLINE = "OFFLINE",
  BIND = "BIND",
  MESSAGE = "MESSAGE",
  UNREAD_MESSAGES = "UNREAD_MESSAGES",
  READ = "READ",
}

export type MessageActionParams = {
  action: MessageAction;
};

export type OnlineOfflineMessage = {
  senderId: number;
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

export type SocketMessageType = OnlineOfflineMessage | BindMessage | MessageLikeMessage | UnreadMessagesMessage | ReadMessage;

export type SocketContextProps = {
  webSocket?: WebSocket | null;
  socketMessage?: SocketMessageType;
};
