"use client";

import { useRouter } from "next/navigation";
import { PropsWithChildren, useEffect, useMemo, useState } from "react";

import { refreshToken } from "@/api/http/auth/auth";
import { connectToSocket } from "@/api/socket";
import { SocketContext } from "@/context/SocketContext/SocketContext";
import { SocketContextProps } from "@/context/SocketContext/socketContext.types";
import { RECONNECT_TIMEOUT } from "@/context/SocketContext/SocketProvider/socketProvider.config";
import { setAccessToken } from "@/lib/features/user/userSlice";
import { useAppDispatch, useAppSelector } from "@/lib/hooks";

export function SocketProvider({ children }: Readonly<PropsWithChildren>) {
  const dispatch = useAppDispatch();
  const { accessToken } = useAppSelector(state => state.auth);
  const route = useRouter();
  const [webSocket, setWebSocket] = useState<WebSocket | null>(null);
  const [message, setMessage] = useState<unknown>();

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
      setWebSocket(newSocket);
    };

    newSocket.onmessage = event => {
      console.log("Message", event.data);
      setMessage(JSON.parse(event.data));
    };

    newSocket.addEventListener("error", () => {});

    return () => {
      if (newSocket.readyState === WebSocket.OPEN) {
        newSocket.close();
      }
    };
  }, [accessToken, dispatch, route]);

  const value = useMemo<SocketContextProps>(() => {
    return {
      webSocket,
      message,
    };
  }, [message, webSocket]);

  return <SocketContext.Provider value={value}>{children}</SocketContext.Provider>;
}
