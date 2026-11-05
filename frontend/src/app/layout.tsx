// Fonts are bundled by Next and served from our own origin (no Google Fonts at runtime)
import "@fontsource/be-vietnam-pro/vietnamese-400.css";
import "@fontsource/be-vietnam-pro/vietnamese-600.css";
import "@fontsource/noto-serif/vietnamese-500.css";
import "@fontsource/noto-serif/vietnamese-600.css";
import "./globals.css";

import type { Metadata } from "next";
import type { ReactNode } from "react";

import { ThemeProvider } from "@/components/theme/ThemeProvider";

export const metadata: Metadata = {
    title: "SGU Chat",
    description: "Chat nội bộ của Đại học Sài Gòn",
};

export default function RootLayout({ children }: { children: ReactNode }) {
    return (
        <html lang="vi" suppressHydrationWarning>
        <body>
        <ThemeProvider>{children}</ThemeProvider>
        </body>
        </html>
    );
}