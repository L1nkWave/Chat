import { createSlice, PayloadAction } from "@reduxjs/toolkit";

interface AuthState {
  accessToken: string | null;
}

const initialState: AuthState = {
  accessToken: null,
};

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    setAccessToken: (state, action: PayloadAction<string | null>) => {
      return { ...state, accessToken: action.payload };
    },
  },
});

export const { setAccessToken } = authSlice.actions;
export const authReducer = authSlice.reducer;
