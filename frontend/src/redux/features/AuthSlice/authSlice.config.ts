import { AuthState } from "@/redux/features/AuthSlice/authSlice.types";

export const AUTH_SLICE_NAME = "auth";

export const authInitialState: AuthState = {
  accessToken: null,
};
