"use client";

import { useRouter } from "next/navigation";
import { PropsWithChildren, useEffect, useMemo, useRef, useState } from "react";

import { refreshToken } from "@/api/http/auth/auth";
import { connectToSocket } from "@/api/socket";
import { SocketContext } from "@/context/SocketContext/SocketContext";
import { SocketContextProps, SocketMessageType } from "@/context/SocketContext/socketContext.types";
import { isTokenExpired } from "@/helpers/DecodeToken/decodeToken";
import { useAppSelector } from "@/lib/hooks";

const MAX_RECONNECT_ATTEMPTS = 5;
const BASE_DELAY_MS = 1000;

export function SocketProvider({ children }: Readonly<PropsWithChildren>) {
  const { accessToken } = useAppSelector(state => state.user);
  const route = useRouter();
  const [webSocket, setWebSocket] = useState<WebSocket | null>(null);
  const [socketMessage, setSocketMessage] = useState<SocketMessageType>();
  const attemptsRef = useRef(0);

  // eslint-disable-next-line sonarjs/cognitive-complexity
  useEffect(() => {
    if (!accessToken) {
      route.push("/sign-in");
      return () => {};
    }

    let active = true;
    let timer: ReturnType<typeof setTimeout> | undefined;
    let currentSocket: WebSocket | null = null;

    const open = (token: string) => {
      if (!active) return;

      const socket = connectToSocket(token);
      currentSocket = socket;
      setWebSocket(socket);

      socket.onopen = () => {
        attemptsRef.current = 0;
      };

      socket.onmessage = event => {
        setSocketMessage(JSON.parse(event.data));
      };

      socket.onclose = () => {
        if (!active) return;

        if (attemptsRef.current >= MAX_RECONNECT_ATTEMPTS) {
          route.push("/sign-in");
          return;
        }

        attemptsRef.current += 1;
        const delay = BASE_DELAY_MS * 2 ** (attemptsRef.current - 1);

        timer = setTimeout(async () => {
          if (!active) return;
          try {
            let nextToken = token;
            if (isTokenExpired(token)) {
              const data = await refreshToken();
              nextToken = data.accessToken;
            }
            open(nextToken);
          } catch (error) {
            console.error("Failed to refresh token:", error);
            route.push("/sign-in");
          }
        }, delay);
      };
    };

    open(accessToken);

    return () => {
      active = false;
      if (timer) clearTimeout(timer);
      if (currentSocket && currentSocket.readyState === WebSocket.OPEN) {
        currentSocket.close();
      }
    };
  }, [accessToken, route]);

  const value = useMemo<SocketContextProps>(() => {
    return {
      webSocket,
      socketMessage,
    };
  }, [socketMessage, webSocket]);

  return <SocketContext.Provider value={value}>{children}</SocketContext.Provider>;
}
