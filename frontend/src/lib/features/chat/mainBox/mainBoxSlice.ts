import { createSlice, PayloadAction } from "@reduxjs/toolkit";

import { MAIN_BOX_SLICE_NAME, mainBoxInitialState } from "@/lib/features/chat/mainBox/mainBoxSlice.config";
import { MainBoxState } from "@/lib/features/chat/mainBox/mainBoxSlice.types";

const mainBoxSlice = createSlice({
  name: MAIN_BOX_SLICE_NAME,
  initialState: mainBoxInitialState,
  reducers: {
    setCurrentMainBox: (state, action: PayloadAction<MainBoxState>) => {
      return { ...state, currentMainBox: action.payload };
    },
  },
});

export const { setCurrentMainBox } = mainBoxSlice.actions;
export const mainBoxReducer = mainBoxSlice.reducer;
