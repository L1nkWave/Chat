"use client";

import { useRouter } from "next/navigation";
import { PropsWithChildren, useEffect } from "react";

import { connectToSocket, disconnectFromSocket } from "@/api/socket";
import { setSocket } from "@/lib/features/socket/socketSlice";
import { useAppDispatch, useAppSelector } from "@/lib/hooks";

export function SocketProvider({ children }: Readonly<PropsWithChildren>) {
  const dispatch = useAppDispatch();
  const { accessToken } = useAppSelector(state => state.auth);
  const route = useRouter();

  useEffect(() => {
    if (!accessToken) return route.push("/sign-in");
    const socket = connectToSocket(accessToken);
    dispatch(setSocket(socket));

    return () => {
      disconnectFromSocket(socket);
    };
  }, [accessToken, dispatch, route]);

  return <div>{children}</div>;
}
