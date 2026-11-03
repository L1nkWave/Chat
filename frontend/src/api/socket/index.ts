import { FileHttpResponse } from "@/api/http/contacts/contacts.types";

const socketUrl = process.env.NEXT_PUBLIC_WEB_SOCKET_URL;

// Các lệnh gửi khi socket chưa mở sẽ được giữ lại và gửi khi kết nối thành công
const pendingFrames: string[] = [];

const flushPending = (socket: WebSocket) => {
  while (pendingFrames.length > 0 && socket.readyState === WebSocket.OPEN) {
    const frame = pendingFrames.shift();
    if (frame !== undefined) {
      socket.send(frame);
    }
  }
};

const sendFrame = (socket: WebSocket, frame: string) => {
  if (socket.readyState === WebSocket.OPEN) {
    socket.send(frame);
  } else {
    pendingFrames.push(frame);
  }
};

export const connectToSocket = (token: string) => {
  const socket = new WebSocket(`${socketUrl}?access=${token}`);
  socket.addEventListener("open", () => {
    console.log("Connected to socket");
    flushPending(socket);
  });
  return socket;
};

export const sendChatMessage = (socket: WebSocket, chatId: string, message: string, tempId: string) => {
  const path = `path=/chat/${chatId}/send`;
  const payload = JSON.stringify({
    tmpMessageId: tempId,
    text: message,
  });

  sendFrame(
    socket,
    `${path}
  
   ${payload}`
  );
};

export const sendFileMessage = (socket: WebSocket, chatId: string, fileHttpResponse: FileHttpResponse) => {
  const path = `path=/chat/${chatId}/send_file`;
  const payload = JSON.stringify({
    id: fileHttpResponse.id,
    createdAt: fileHttpResponse.createdAt,
    filename: fileHttpResponse.filename,
    contentType: fileHttpResponse.contentType,
    size: fileHttpResponse.size,
  });

  sendFrame(
    socket,
    `${path}
  
   ${payload}`
  );
};

export const checkUnreadMessages = (socket: WebSocket) => {
  const path = `path=/chat/unread`;
  sendFrame(socket, path);
};

export const readMessages = (socket: WebSocket, chatId: string, timestamp: number) => {
  const path = `path=/chat/${chatId}/read`;
  const payload = JSON.stringify({ timestamp });
  sendFrame(
    socket,
    `${path}
  
    ${payload}
  `
  );
};

export const addMemberToGroupChat = (socket: WebSocket, chatId: string, userId: number) => {
  const path = `path=/chat/group/${chatId}/add_member/${userId}`;
  sendFrame(socket, `${path}`);
};
