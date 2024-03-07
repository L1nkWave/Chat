import { AuthStateParams } from "@/lib/features/auth/authSlice.types";

export const AUTH_SLICE_NAME = "auth";

export const authInitialState: AuthStateParams = {
  accessToken: null,
};
