"use client";

import { AdminConsole } from "@/components/admin/AdminConsole";
import { useAuthGate } from "@/lib/auth/useAuthGate";

export default function AdminPage() {
    const { status, isRoot } = useAuthGate(true, true);

    if (status === "ready") {
        return <AdminConsole isRoot={isRoot} />;
    }

    const text = status === "forbidden" ? "Bạn không có quyền truy cập trang quản trị." : "Đang kiểm tra phiên đăng nhập...";
    return (
        <main className="grid min-h-dvh place-items-center bg-paper px-4 text-center">
            <p className="text-sm text-muted">{text}</p>
        </main>
    );
}