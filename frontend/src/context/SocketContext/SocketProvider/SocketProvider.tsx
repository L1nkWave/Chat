"use client";

import { useRouter } from "next/navigation";
import { PropsWithChildren, useEffect, useMemo, useState } from "react";

import { refreshToken } from "@/api/http/auth/auth";
import { AuthTypes } from "@/api/http/auth/auth.types";
import { connectToSocket } from "@/api/socket";
import { SocketContext } from "@/context/SocketContext/SocketContext";
import { SocketContextProps, SocketMessageType } from "@/context/SocketContext/socketContext.types";
import { isTokenExpired } from "@/helpers/DecodeToken/decodeToken";
import { useAppDispatch, useAppSelector } from "@/lib/hooks";

const MAX_RECONNECT_ATTEMPTS = 3;

export function SocketProvider({ children }: Readonly<PropsWithChildren>) {
  const dispatch = useAppDispatch();
  const { accessToken } = useAppSelector(state => state.user);
  const route = useRouter();
  const [webSocket, setWebSocket] = useState<WebSocket | null>(null);
  const [socketMessage, setSocketMessage] = useState<SocketMessageType>();
  const [reconnectAttempts, setReconnectAttempts] = useState(0);

  useEffect(() => {
    if (!accessToken) {
      route.push("/sign-in");
      return () => {};
    }

    const newSocket = connectToSocket(accessToken);

    newSocket.onclose = async () => {
      if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
        try {
          let data: AuthTypes = { accessToken, refreshExpiration: 0 };
          if (isTokenExpired(accessToken)) {
            data = await refreshToken();
          }
          const newWebSocket = connectToSocket(data.accessToken);
          setWebSocket(newWebSocket);
          setReconnectAttempts(prevAttempts => prevAttempts + 1);
        } catch (error) {
          console.error("Failed to refresh token:", error);
          route.push("/sign-in");
        }
      } else {
        route.push("/sign-in");
      }
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
  }, [accessToken, dispatch, route, reconnectAttempts]);

  const value = useMemo<SocketContextProps>(() => {
    return {
      webSocket,
      socketMessage,
    };
  }, [socketMessage, webSocket]);

  return <SocketContext.Provider value={value}>{children}</SocketContext.Provider>;
}
