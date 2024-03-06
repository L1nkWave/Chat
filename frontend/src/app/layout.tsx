import "./globals.css";
import "react-toastify/dist/ReactToastify.css";

import type { Metadata } from "next";
import localFont from "next/font/local";
import React from "react";

import { ToastManager } from "@/components/ToastManager/ToastManager";
import StoreProvider from "@/context/StoreProvider/StoreProvider";
import { ThemeProvider } from "@/context/ThemeProvider/ThemeProvider";

const ggSans = localFont({
  src: [
    {
      path: "../../public/fonts/gg-sans/gg_sans_Bold.woff",
      weight: "700",
      style: "bold",
    },
    {
      path: "../../public/fonts/gg-sans/gg_sans_Medium.woff",
      weight: "500",
      style: "medium",
    },
    {
      path: "../../public/fonts/gg-sans/gg_sans_Regular.woff",
      weight: "400",
      style: "regular",
    },
    {
      path: "../../public/fonts/gg-sans/gg_sans_Semibold.woff",
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
      <body
        className={`${ggSans.className} bg-white h-screen dark:bg-dark-400 dark:text-white`}
      >
        <StoreProvider>
          <ThemeProvider>
            <ToastManager />
            {children}
          </ThemeProvider>
        </StoreProvider>
      </body>
    </html>
  );
}
