import { createSlice, PayloadAction } from "@reduxjs/toolkit";

import { UserParams } from "@/api/http/contacts/contacts.types";
import { USER_SLICE_NAME, userInitialState } from "@/lib/features/user/userSlice.config";

const userSlice = createSlice({
  name: USER_SLICE_NAME,
  initialState: userInitialState,
  reducers: {
    setAccessToken: (state, action: PayloadAction<string | null>) => {
      return { ...state, accessToken: action.payload };
    },
    setCurrentUser: (state, action: PayloadAction<UserParams | null>) => {
      return { ...state, currentUser: action.payload };
    },
  },
});

export const { setAccessToken, setCurrentUser } = userSlice.actions;
export const userReducer = userSlice.reducer;
