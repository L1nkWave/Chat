import { createSlice, PayloadAction } from "@reduxjs/toolkit";

import { USER_SLICE_NAME, userInitialState } from "@/lib/features/user/userSlice.config";

const userSlice = createSlice({
  name: USER_SLICE_NAME,
  initialState: userInitialState,
  reducers: {
    setAccessToken: (state, action: PayloadAction<string | null>) => {
      return { ...state, accessToken: action.payload };
    },
  },
});

export const { setAccessToken } = userSlice.actions;
export const authReducer = userSlice.reducer;
