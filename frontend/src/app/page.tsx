import Link from "next/link";

import { Logo } from "@/components/brand/Logo";
import { ThemeToggle } from "@/components/theme/ThemeToggle";
import { buttonClasses } from "@/components/ui/Button";

const sampleMessages = [
    { id: 1, author: "Nhóm đồ án DCT1222", mine: false, text: "Cả nhóm họp lúc 14:00 chiều nay nhé." },
    { id: 2, author: "Bạn", mine: true, text: "Mình sẽ gửi bản phân công trước 12:00." },
    { id: 3, author: "Giảng viên hướng dẫn", mine: false, text: "Lịch nộp báo cáo giữa kỳ đã được cập nhật trong nhóm lớp." },
];

export default function HomePage() {
    return (
        <div className="flex min-h-dvh flex-col bg-paper text-ink">
            <header className="mx-auto flex w-full max-w-6xl items-center justify-between px-4 py-5 sm:px-8">
                <Link href="/" className="flex items-center gap-3 rounded-md focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary">
                    <Logo size={36} />
                    <span className="font-serif text-lg font-semibold">SGU Chat</span>
                </Link>
                <div className="flex items-center gap-2">
                    <ThemeToggle />
                    <Link href="/login" className={buttonClasses("outline", "hidden sm:inline-flex")}>
                        Đăng nhập
                    </Link>
                </div>
            </header>

            <main className="mx-auto grid w-full max-w-6xl flex-1 items-center gap-14 px-4 pb-20 pt-8 sm:px-8 lg:grid-cols-[1fr_1.05fr] lg:pt-16">
                <section className="flex flex-col gap-8">
                    <h1 className="font-serif text-[40px] font-semibold leading-[1.12] tracking-[-0.02em] sm:text-[52px]">
                        Chat nội bộ của Đại học Sài Gòn
                    </h1>
                    <p className="max-w-[46ch] text-[17px] leading-relaxed text-muted">
                        Nhắn tin giữa sinh viên, giảng viên và các nhóm học tập trong cùng một nơi. Thành viên chỉ vào nhóm
                        khi đã được mời và đồng ý.
                    </p>
                    <div className="flex flex-col gap-3 sm:flex-row">
                        <Link href="/login" className={buttonClasses("primary")}>
                            Đăng nhập
                        </Link>
                        <Link href="/signup" className={buttonClasses("outline")}>
                            Tạo tài khoản
                        </Link>
                    </div>
                </section>

                <section aria-label="Ví dụ cuộc trò chuyện" className="rounded-lg border border-line bg-surface">
                    <div className="flex items-center gap-3 border-b border-line px-5 py-4">
                        <span className="grid size-9 place-items-center rounded-md bg-tint font-semibold text-primary">
                            DC
                        </span>
                        <div className="flex flex-col">
                            <span className="font-medium">Nhóm đồ án DCT1222</span>
                            <span className="text-sm text-muted">3 thành viên</span>
                        </div>
                    </div>

                    <ul className="flex flex-col gap-4 px-5 py-6">
                        {sampleMessages.map((message) => (
                            <li key={message.id} className={`flex flex-col gap-1 ${message.mine ? "items-end" : "items-start"}`}>
                                <span className="text-sm text-muted">{message.author}</span>
                                <p
                                    className={`max-w-[32ch] rounded-md px-4 py-2.5 text-[15px] leading-relaxed ${
                                        message.mine ? "bg-tint text-ink" : "border border-line text-ink"
                                    }`}
                                >
                                    {message.text}
                                </p>
                            </li>
                        ))}
                    </ul>
                </section>
            </main>
        </div>
    );
}