import { createSlice, PayloadAction } from "@reduxjs/toolkit";

import { InteractiveListVariant } from "@/components/Chat/InteractiveList/interactiveList.types";
import { MainBoxVariant } from "@/components/Chat/MainBox/mainBox.types";
import { CHAT_SLICE_NAME, chatInitialState } from "@/lib/features/chat/chatSlice.config";

const chatSlice = createSlice({
  name: CHAT_SLICE_NAME,
  initialState: chatInitialState,
  reducers: {
    setCurrentMainBox: (state, action: PayloadAction<MainBoxVariant>) => {
      return { ...state, currentMainBox: action.payload };
    },
    setCurrentInteractiveList: (state, action: PayloadAction<InteractiveListVariant>) => {
      return { ...state, currentInteractiveList: action.payload };
    },
  },
});

export const { setCurrentMainBox, setCurrentInteractiveList } = chatSlice.actions;
export const chatReducer = chatSlice.reducer;
