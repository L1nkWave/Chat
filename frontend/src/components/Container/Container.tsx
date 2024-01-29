import React, { PropsWithChildren } from "react";

import { Header } from "@/components/Header/Header";

export function Container({ children }: Readonly<PropsWithChildren>) {
  return (
    <>
      <Header withoutEffects logoLabel="LinkWave" />
      <div className="flex flex-col items-center justify-center p-24 h-screen">
        {children}
      </div>
    </>
  );
}
