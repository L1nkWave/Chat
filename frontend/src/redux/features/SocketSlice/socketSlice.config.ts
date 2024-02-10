import { SocketStateTypes } from "@/redux/features/SocketSlice/socketSlice.types";

export const SOCKET_SLICE_NAME = "socket";
export const socketInitialState: SocketStateTypes = {
  webSocket: null,
};
