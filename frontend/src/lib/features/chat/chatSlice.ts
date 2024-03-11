import { createSlice, PayloadAction } from "@reduxjs/toolkit";

import { CHAT_SLICE_NAME, chatInitialState } from "@/lib/features/chat/chatSlice.config";
import { MainBoxStateParams, UserItemStateParams } from "@/lib/features/chat/chatSlice.types";

const chatSlice = createSlice({
  name: CHAT_SLICE_NAME,
  initialState: chatInitialState,
  reducers: {
    setCurrentMainBox: (state, action: PayloadAction<MainBoxStateParams>) => {
      return { ...state, currentMainBox: action.payload };
    },
    setCurrentUserItem: (state, action: PayloadAction<UserItemStateParams>) => {
      return { ...state, currentUserItem: action.payload };
    },
  },
});

export const { setCurrentMainBox, setCurrentUserItem } = chatSlice.actions;
export const chatReducer = chatSlice.reducer;
