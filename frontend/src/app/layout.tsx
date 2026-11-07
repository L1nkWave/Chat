import type { Metadata } from "next";
import { headers } from "next/headers";
import "@fontsource/be-vietnam-pro/latin-400.css";
import "@fontsource/be-vietnam-pro/latin-600.css";
import "@fontsource/be-vietnam-pro/vietnamese-400.css";
import "@fontsource/be-vietnam-pro/vietnamese-600.css";
import "@fontsource/noto-serif/latin-500.css";
import "@fontsource/noto-serif/latin-600.css";
import "@fontsource/noto-serif/vietnamese-500.css";
import "@fontsource/noto-serif/vietnamese-600.css";
import "./globals.css";
import { ThemeProvider } from "@/components/theme/ThemeProvider";

export const metadata: Metadata = {
    title: "SGU Chat",
};

// Fonts are bundled by Next and served from our own origin (no Google Fonts at runtime).
// Each family needs both the latin and vietnamese subsets: the vietnamese subset
// covers only diacritics, so basic Latin letters would otherwise fall back to a system font.
export default async function RootLayout({
                                             children,
                                         }: Readonly<{ children: React.ReactNode }>) {
    // Nonce do middleware tạo, dùng cho script của next-themes.
    const nonce = (await headers()).get("x-nonce") ?? undefined;

    return (
        <html lang="vi" suppressHydrationWarning>
        <body className="antialiased">
        <ThemeProvider nonce={nonce}>{children}</ThemeProvider>
        </body>
        </html>
    );
}