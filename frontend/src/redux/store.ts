import { configureStore } from "@reduxjs/toolkit";

import { authReducer } from "@/redux/features/AuthSlice/authSlice";
import { socketReducer } from "@/redux/features/SocketSlice/socketSlice";

export const makeStore = () =>
  configureStore({
    reducer: {
      auth: authReducer,
      socket: socketReducer,
    },
  });

export type AppStore = ReturnType<typeof makeStore>;
export type RootState = ReturnType<AppStore["getState"]>;
export type AppDispatch = AppStore["dispatch"];
