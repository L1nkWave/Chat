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
            <header className="mx-auto flex w-full max-w-5xl items-center justify-between px-4 py-4 sm:px-8">
                <Link
                    href="/"
                    className="flex items-center gap-3 rounded-md focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary"
                >
                    <Logo size={36} />
                    <span className="font-serif text-lg font-semibold">SGU Chat</span>
                </Link>
                <ThemeToggle />
            </header>

            <main className="flex flex-1 items-start justify-center px-4 pb-16 pt-6 sm:items-center sm:pt-10">
                <div className="flex w-full max-w-[420px] flex-col gap-7 rounded-lg border border-line bg-surface p-6 shadow-[0_1px_0_rgba(13,37,56,0.04),0_24px_48px_-32px_rgba(13,37,56,0.25)] sm:p-9">
                    <div className="flex flex-col gap-2">
                        <h1 className="font-serif text-[28px] font-semibold leading-tight tracking-[-0.01em]">
                            {title}
                        </h1>
                        <p className="text-[15px] leading-relaxed text-muted">{subtitle}</p>
                    </div>

                    {children}

                    <div className="border-t border-line pt-5 text-sm text-muted">{footer}</div>
                </div>
            </main>
        </div>
    );
}