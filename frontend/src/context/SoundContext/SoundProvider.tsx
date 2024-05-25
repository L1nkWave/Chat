"use client";

import debounce from "debounce";
import { PropsWithChildren, useMemo } from "react";

import { SoundContext } from "@/context/SoundContext/SoundContext";

export function SocketProvider({ children }: Readonly<PropsWithChildren>) {
  const messageSound = () => {
    new Audio("/sound/message.mp3").play();
  };

  const debouncedMessageSound = useMemo(() => debounce(messageSound, 100), []);

  const soundContextValue = useMemo(() => ({ messageSound: debouncedMessageSound }), [debouncedMessageSound]);

  return <SoundContext.Provider value={soundContextValue}>{children}</SoundContext.Provider>;
}
