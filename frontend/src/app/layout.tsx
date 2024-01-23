import "./globals.css";

import type { Metadata } from "next";
import localFont from "next/font/local";
import React from "react";

import StoreProvider from "@/context/StoreProvider/StoreProvider";
import { ThemeProvider } from "@/context/ThemeProvider/ThemeProvider";

const ggSans = localFont({
  src: [
    {
      path: "../../public/fonts/gg-sans/gg sans Bold.woff",
      weight: "700",
      style: "bold",
    },
    {
      path: "../../public/fonts/gg-sans/gg sans Medium.woff",
      weight: "500",
      style: "medium",
    },
    {
      path: "../../public/fonts/gg-sans/gg sans Regular.woff",
      weight: "400",
      style: "regular",
    },
    {
      path: "../../public/fonts/gg-sans/gg sans Semibold.woff",
      weight: "600",
      style: "semibold",
    },
  ],
});
export const metadata: Metadata = {
  title: "Link Wave Chat",
  description: "Chat with your friends in a new way.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="no-scrollbar" suppressHydrationWarning>
      <body className={ggSans.className}>
        <StoreProvider>
          <ThemeProvider>{children}</ThemeProvider>
        </StoreProvider>
      </body>
    </html>
  );
}
