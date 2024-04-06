import { ChatType } from "@/api/socket/index.types";

const socketUrl = process.env.NEXT_PUBLIC_WEB_SOCKET_URL;

export const connectToSocket = (token: string) => {
  return new WebSocket(`${socketUrl}?access=${token}`);
};

export const sendChatMessage = (socket: WebSocket, message: string, chatType: ChatType, chatId: number | string) => {
  socket.send(`path=/${chatType}/${chatId}/send
  
  
    ${message}
  `);
};
