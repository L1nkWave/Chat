"use client";

import { useRouter } from "next/navigation";
import { PropsWithChildren, useEffect, useMemo, useState } from "react";

import { connectToSocket } from "@/api/socket";
import { SocketContext } from "@/context/SocketContext/SocketContext";
import { RECONNECT_TIMEOUT } from "@/context/SocketContext/SocketProvider/socketProvider.config";
import { useAppDispatch, useAppSelector } from "@/lib/hooks";

export function SocketProvider({ children }: Readonly<PropsWithChildren>) {
  const dispatch = useAppDispatch();
  const { accessToken } = useAppSelector(state => state.auth);
  const route = useRouter();
  const [webSocket, setWebSocket] = useState<WebSocket | null>(null);

  useEffect(() => {
    if (!accessToken) {
      route.push("/sign-in");
      return () => {};
    }
    const newSocket = connectToSocket(accessToken);
    newSocket.onclose = () => {
      setTimeout(async () => {
        try {
          const refreshedSocket = connectToSocket(accessToken);
          setWebSocket(refreshedSocket);
        } catch (error) {
          setWebSocket(null);
        }
      }, RECONNECT_TIMEOUT);
    };

    newSocket.onerror = () => {
      route.push("/sign-in");
    };

    newSocket.onopen = () => {
      setWebSocket(newSocket);
    };

    return () => {
      if (newSocket.readyState === WebSocket.OPEN) {
        newSocket.close();
      }
    };
  }, [accessToken, dispatch, route]);

  const value = useMemo(() => {
    return {
      webSocket,
    };
  }, [webSocket]);

  return <SocketContext.Provider value={value}>{children}</SocketContext.Provider>;
}
