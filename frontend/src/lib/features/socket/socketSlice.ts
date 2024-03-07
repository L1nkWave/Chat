import { createSlice, PayloadAction } from "@reduxjs/toolkit";

import { SOCKET_SLICE_NAME, socketInitialState } from "@/lib/features/socket/socketSlice.config";

const socketSlice = createSlice({
  name: SOCKET_SLICE_NAME,
  initialState: socketInitialState,
  reducers: {
    setSocket: (state, action: PayloadAction<WebSocket | null>) => {
      return { ...state, webSocket: action.payload };
    },
  },
});

export const { setSocket } = socketSlice.actions;
export const socketReducer = socketSlice.reducer;
