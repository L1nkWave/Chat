"use client";

import { PropsWithChildren } from "react";

import { SoundContext } from "@/context/SoundContext/SoundContext";

export function SocketProvider({ children }: Readonly<PropsWithChildren>) {
  const messageSound = () => {
    new Audio("/sound/message.mp3").play();
  };
  const value = { messageSound };

  return <SoundContext.Provider value={value}>{children}</SoundContext.Provider>;
}
