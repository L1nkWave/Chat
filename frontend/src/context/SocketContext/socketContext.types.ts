export enum MessageAction {
  ONLINE = "ONLINE",
  OFFLINE = "OFFLINE",
  BIND = "BIND",
  MESSAGE = "MESSAGE",
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

export type SocketMessageType = OnlineOfflineMessage | BindMessage | MessageLikeMessage;

export type SocketContextProps = {
  webSocket?: WebSocket | null;
  socketMessage?: SocketMessageType;
};
