import Link from "next/link";

import { buttonClasses } from "@/components/ui/Button";

// Placeholder until the chat screens are rebuilt. Sign-in redirects here.
export default function ChatPlaceholderPage() {
    return (
        <main className="mx-auto flex min-h-dvh max-w-[420px] flex-col justify-center gap-5 px-4 text-ink">
            <h1 className="font-serif text-[28px] font-semibold">Giao diện chat đang được xây dựng lại</h1>
            <p className="text-[15px] text-muted">
                Bạn đã đăng nhập thành công. Màn hình trò chuyện sẽ có trong phiên bản tiếp theo.
            </p>
            <Link href="/" className={buttonClasses("outline", "w-fit")}>
                Về trang chủ
            </Link>
        </main>
    );
}