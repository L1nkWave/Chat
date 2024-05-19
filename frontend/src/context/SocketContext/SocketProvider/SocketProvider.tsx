"use client";

import { useRouter } from "next/navigation";
import { PropsWithChildren, useEffect, useMemo, useState } from "react";

import { refreshToken } from "@/api/http/auth/auth";
import { connectToSocket } from "@/api/socket";
import { SocketContext } from "@/context/SocketContext/SocketContext";
import { SocketContextProps, SocketMessageType } from "@/context/SocketContext/socketContext.types";
import { RECONNECT_TIMEOUT } from "@/context/SocketContext/SocketProvider/socketProvider.config";
import { setAccessToken } from "@/lib/features/user/userSlice";
import { useAppDispatch, useAppSelector } from "@/lib/hooks";

export function SocketProvider({ children }: Readonly<PropsWithChildren>) {
  const dispatch = useAppDispatch();
  const { accessToken } = useAppSelector(state => state.user);
  const route = useRouter();
  const [webSocket, setWebSocket] = useState<WebSocket | null>(null);
  const [socketMessage, setSocketMessage] = useState<SocketMessageType>();

  useEffect(() => {
    if (!accessToken) {
      route.push("/sign-in");
      return () => {};
    }

    const newSocket = connectToSocket(accessToken);

    newSocket.onclose = () => {
      setTimeout(async () => {
        try {
          const data = await refreshToken();
          dispatch(setAccessToken(data.accessToken));
        } catch (error) {
          setWebSocket(null);
        }
      }, RECONNECT_TIMEOUT);
    };

    newSocket.onerror = () => {
      console.log("Error");
      route.push("/sign-in");
    };

    newSocket.onopen = event => {
      console.log("Open", event);
    };

    newSocket.onmessage = event => {
      setSocketMessage(JSON.parse(event.data));
    };

    setWebSocket(newSocket);

    return () => {
      if (newSocket.readyState === WebSocket.OPEN) {
        newSocket.close();
      }
    };
  }, [accessToken, dispatch, route]);

  const value = useMemo<SocketContextProps>(() => {
    return {
      webSocket,
      socketMessage,
    };
  }, [socketMessage, webSocket]);

  return <SocketContext.Provider value={value}>{children}</SocketContext.Provider>;
}
