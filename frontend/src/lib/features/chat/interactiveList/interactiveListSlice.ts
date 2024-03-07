import { createSlice, PayloadAction } from "@reduxjs/toolkit";

import {
  INTERACTIVE_LIST_SLICE_NAME,
  interactiveListInitialState,
} from "@/lib/features/chat/interactiveList/interactiveListSlice.config";
import { InteractiveListState } from "@/lib/features/chat/interactiveList/interactiveListSlice.types";

const interactiveListSlice = createSlice({
  name: INTERACTIVE_LIST_SLICE_NAME,
  initialState: interactiveListInitialState,
  reducers: {
    setCurrentInteractiveList: (state, action: PayloadAction<InteractiveListState>) => {
      return { ...state, currentInteractiveList: action.payload };
    },
  },
});

export const { setCurrentInteractiveList } = interactiveListSlice.actions;
export const interactiveListReducer = interactiveListSlice.reducer;
