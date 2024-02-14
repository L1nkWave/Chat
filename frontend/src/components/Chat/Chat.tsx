"use client";

import Image from "next/image";
import { useRouter } from "next/navigation";
import React, { useEffect } from "react";

import { ChatList } from "@/components/Chat/ChatList/ChatList";
import { useAppSelector } from "@/redux/hooks";
import { decodeToken } from "@/utils/DecodeToken/decodeToken";

export function Chat() {
  const { accessToken } = useAppSelector(state => state.auth);
  const { webSocket } = useAppSelector(state => state.socket);
  const router = useRouter();

  useEffect(() => {
    if (!accessToken) {
      return router.push("/sign-in");
    }
    if (!webSocket) {
      return () => {};
    }

    const payload = decodeToken(accessToken);
    if (!payload) {
      return router.push("/sign-in");
    }

    webSocket.addEventListener("open", event => {
      console.log("WebSocket connection opened:", event);
    });

    webSocket.addEventListener("message", event => {
      console.log("WebSocket connection message:", event);
    });

    webSocket.addEventListener("close", event => {
      console.log("WebSocket connection closed:", event);
    });

    return () => {
      if (!webSocket) return;
      webSocket.removeEventListener("open", () => {});
      webSocket.removeEventListener("message", () => {});
      webSocket.removeEventListener("close", () => {});
    };
  }, [accessToken, router, webSocket]);
  if (webSocket?.readyState === WebSocket.CLOSED) {
    return (
      <div className="w-full h-full">
        <Image
          width={400}
          height={500}
          className="w-full rounded-lg"
          src="/images/ChatPage/backend-fall.gif"
          alt="Backend is currently unstable, please wait."
        />
      </div>
    );
  }

  return (
    <div className="w-screen flex px-64">
      <div className="flex flex-col w-1/8 bg-dark-250 p-6 h-screen rounded-l-2xl">
        LW
      </div>
      <ChatList />
      <div className="flex w-full bg-dark-550 p-2 h-screen items-center justify-center rounded-r-2xl">
        Choose chat room to start chatting
      </div>
    </div>
  );
}
