import Link from "next/link";
import type { ReactNode } from "react";

import { Logo } from "@/components/brand/Logo";
import { ThemeToggle } from "@/components/theme/ThemeToggle";

type AuthLayoutProps = {
    title: string;
    subtitle: string;
    children: ReactNode;
    footer: ReactNode;
};

export function AuthLayout({ title, subtitle, children, footer }: AuthLayoutProps) {
    return (
        <div className="flex min-h-dvh flex-col bg-paper text-ink">
            <header className="mx-auto flex w-full max-w-6xl items-center justify-between px-4 py-5 sm:px-8">
                <Link href="/" className="flex items-center gap-3 rounded-md focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary">
                    <Logo size={36} />
                    <span className="font-serif text-lg font-semibold">SGU Chat</span>
                </Link>
                <ThemeToggle />
            </header>

            <main className="mx-auto flex w-full max-w-[380px] flex-1 flex-col gap-8 px-4 pb-16 pt-8 sm:pt-14">
                <div className="flex flex-col gap-2">
                    <h1 className="font-serif text-[32px] font-semibold leading-tight">{title}</h1>
                    <p className="text-[15px] text-muted">{subtitle}</p>
                </div>

                {children}

                <div className="text-sm text-muted">{footer}</div>
            </main>
        </div>
    );
}