import { createSlice, PayloadAction } from "@reduxjs/toolkit";

import { InteractiveListVariant } from "@/components/Chat/InteractiveList/interactiveList.types";
import { MainBoxVariant } from "@/components/Chat/MainBox/mainBox.types";
import { CHAT_SLICE_NAME, chatInitialState } from "@/lib/features/chat/chatSlice.config";

const chatSlice = createSlice({
  name: CHAT_SLICE_NAME,
  initialState: chatInitialState,
  reducers: {
    setCurrentMainBoxState: (state, action: PayloadAction<MainBoxVariant>) => {
      return { ...state, currentMainBoxState: action.payload };
    },
    setCurrentInteractiveListState: (state, action: PayloadAction<InteractiveListVariant>) => {
      return { ...state, currentInteractiveListState: action.payload };
    },
  },
});

export const { setCurrentMainBoxState, setCurrentInteractiveListState } = chatSlice.actions;
export const chatReducer = chatSlice.reducer;
