import { configureStore } from "@reduxjs/toolkit";

import { authReducer } from "@/lib/features/auth/authSlice";
import { chatReducer } from "@/lib/features/chat/chatSlice";
import { socketReducer } from "@/lib/features/socket/socketSlice";

export const makeStore = () =>
  configureStore({
    reducer: {
      auth: authReducer,
      socket: socketReducer,
      chat: chatReducer,
    },
    middleware: getDefaultMiddleware =>
      getDefaultMiddleware({
        serializableCheck: false,
      }),
  });

export const store = makeStore();

export type AppStore = ReturnType<typeof makeStore>;
export type RootState = ReturnType<AppStore["getState"]>;
export type AppDispatch = AppStore["dispatch"];
