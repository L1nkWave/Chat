import React, { PropsWithChildren } from "react";

import { ContainerProps } from "@/components/Container/container.types";
import { Header } from "@/components/Header/Header";

export function Container({ children, showHeader = true }: Readonly<PropsWithChildren<ContainerProps>>) {
  return (
    <>
      {showHeader ? <Header withoutEffects logoLabel="LinkWave" /> : null}
      <div className="flex flex-col items-center justify-center p-24 h-screen">{children}</div>
    </>
  );
}
