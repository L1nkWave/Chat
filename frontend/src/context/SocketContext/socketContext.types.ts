export enum MessageAction {
  ONLINE = "ONLINE",
  OFFLINE = "OFFLINE",
}

export type MessageContextParams = {
  action: MessageAction;
  senderId: number;
  timestamp: string;
};

export type SocketContextProps = {
  webSocket?: WebSocket | null;
  message?: MessageContextParams;
};
