"use client";

import { useRouter } from "next/navigation";
import { PropsWithChildren, useCallback, useEffect } from "react";

import { connectToSocket } from "@/api/socket";
import { RECONNECT_TIMEOUT } from "@/context/SocketProvider/socketProvider.config";
import { setSocket } from "@/lib/features/socket/socketSlice";
import { useAppDispatch, useAppSelector } from "@/lib/hooks";

export function SocketProvider({ children }: Readonly<PropsWithChildren>) {
  const dispatch = useAppDispatch();
  const { webSocket } = useAppSelector(state => state.socket);
  const { accessToken } = useAppSelector(state => state.auth);
  const route = useRouter();

  const socketCreation = useCallback(() => {
    if (!accessToken) {
      route.push("/sign-in");
      return;
    }
    const newSocket = connectToSocket(accessToken);

    newSocket.onclose = () => {
      setTimeout(() => {
        socketCreation();
      }, RECONNECT_TIMEOUT);
    };

    newSocket.onerror = () => {
      newSocket.close();
    };

    dispatch(setSocket(newSocket));
  }, [accessToken, route, dispatch]);

  useEffect(() => {
    socketCreation();
    return () => {
      if (webSocket) {
        webSocket.close();
      }
    };
  }, [accessToken, route, dispatch, socketCreation]);

  return <div>{children}</div>;
}
