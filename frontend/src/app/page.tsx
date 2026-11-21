import Link from "next/link";

import { Logo } from "@/components/brand/Logo";
import { ThemeToggle } from "@/components/theme/ThemeToggle";
import { buttonClasses } from "@/components/ui/Button";

export default function HomePage() {
    return (
        <div className="flex min-h-dvh flex-col bg-paper text-ink">
            <header className="mx-auto flex w-full max-w-2xl items-center justify-between px-4 py-4 sm:px-8">
                <Link
                    href="/"
                    className="flex items-center gap-3 rounded-md focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary"
                >
                    <Logo size={36} />
                    <span className="font-serif text-lg font-semibold">SGU Chat</span>
                </Link>
                <div className="flex items-center gap-2">
                    <ThemeToggle />
                    <Link href="/login" className={buttonClasses("secondary", "hidden sm:inline-flex")}>
                        Đăng nhập
                    </Link>
                </div>
            </header>

            <main className="mx-auto flex w-full max-w-2xl flex-1 flex-col justify-center px-4 pb-20 pt-10 sm:px-8">
                <section className="flex flex-col gap-5">
                    <h1 className="text-4xl font-semibold leading-tight tracking-tight text-balance text-ink sm:text-5xl">
                        Chat nội bộ của Đại học Sài Gòn
                    </h1>
                    <p className="max-w-[60ch] text-base leading-relaxed text-muted">
                        Nhắn tin giữa sinh viên, giảng viên và các nhóm học tập trong cùng một nơi. Thành viên chỉ vào nhóm
                        khi đã được mời và đồng ý.
                    </p>
                    <p className="text-sm leading-relaxed text-muted">
                        Tài khoản do nhà trường cấp. Không có đăng ký công khai.
                    </p>
                    <Link href="/login" className={buttonClasses("primary", "self-start")}>
                        Đăng nhập
                    </Link>
                </section>
            </main>
        </div>
    );
}