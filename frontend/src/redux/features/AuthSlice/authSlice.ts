import { createSlice, PayloadAction } from "@reduxjs/toolkit";

import {
  AUTH_SLICE_NAME,
  authInitialState,
} from "@/redux/features/AuthSlice/authSlice.config";

const authSlice = createSlice({
  name: AUTH_SLICE_NAME,
  initialState: authInitialState,
  reducers: {
    setAccessToken: (state, action: PayloadAction<string | null>) => {
      return { ...state, accessToken: action.payload };
    },
  },
});

export const { setAccessToken } = authSlice.actions;
export const authReducer = authSlice.reducer;
