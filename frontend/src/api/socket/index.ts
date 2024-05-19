const socketUrl = process.env.NEXT_PUBLIC_WEB_SOCKET_URL;

export const connectToSocket = (token: string) => {
  const socket = new WebSocket(`${socketUrl}?access=${token}`);
  socket.onopen = () => {
    console.log("Connected to socket");
  };
  return socket;
};

export const sendChatMessage = (socket: WebSocket, chatId: string, message: string, tempId: string) => {
  const path = `path=/chat/${chatId}/send`;
  const payload = JSON.stringify({
    tmpMessageId: tempId,
    text: message,
  });

  socket.send(`${path}
  
   ${payload}`);
};
