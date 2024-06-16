"use client";

import { ThemeProvider as Provider } from "next-themes";
import React, { PropsWithChildren } from "react";

export function ThemeProvider({ children }: Readonly<PropsWithChildren>) {
  return (
    <Provider attribute="class" defaultTheme="system" enableSystem>
      {children}
    </Provider>
  );
}
