import { SocketStateParams } from "@/lib/features/socket/socketSlice.types";

export const SOCKET_SLICE_NAME = "socket";
export const socketInitialState: SocketStateParams = {
  webSocket: null,
};
