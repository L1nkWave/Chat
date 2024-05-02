import { ChatType } from "@/api/socket/index.types";

const socketUrl = process.env.NEXT_PUBLIC_WEB_SOCKET_URL;

export const connectToSocket = (token: string) => {
  const socket = new WebSocket(`${socketUrl}?access=${token}`);
  socket.onopen = () => {
    console.log("Connected to socket");
  };
  return socket;
};

export const sendChatMessage = (socket: WebSocket, message: string, chatType: ChatType, chatId: number | string) => {
  socket.send(`path=/chat/${chatId}/send
  
  
    ${message}
  `);
};
