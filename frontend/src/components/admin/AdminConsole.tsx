"use client";

import Link from "next/link";
import { useState } from "react";

import { ThemeToggle } from "@/components/theme/ThemeToggle";
import { AcademicTab } from "./AcademicTab";
import { AuditTab } from "./AuditTab";
import { PermissionsTab } from "./PermissionsTab";
import { ReportsTab } from "./ReportsTab";
import { RolesTab } from "./RolesTab";
import { UsersTab } from "./UsersTab";

type TabId = "reports" | "users" | "academic" | "roles" | "permissions" | "audit";

type AdminConsoleProps = {
    isRoot: boolean;
};

export function AdminConsole({ isRoot }: AdminConsoleProps) {
    const [tab, setTab] = useState<TabId>("reports");

    // Nhóm chính: mọi quản trị viên. Nhóm phân quyền: chỉ ROOT. Nhật ký luôn ở cuối.
    const tabs: { id: TabId; label: string }[] = [
        { id: "reports", label: "Báo cáo" },
        { id: "users", label: "Người dùng" },
        { id: "academic", label: "Học vụ" },
        ...(isRoot
            ? [
                { id: "roles" as const, label: "Vai trò" },
                { id: "permissions" as const, label: "Phân quyền" },
            ]
            : []),
        { id: "audit", label: "Nhật ký" },
    ];

    return (
        <div className="flex min-h-dvh flex-col bg-paper text-ink">
            <header className="border-b border-line bg-surface">
                <div className="mx-auto flex w-full max-w-6xl flex-wrap items-center justify-between gap-3 px-4 pt-5 sm:px-8">
                    <div className="flex flex-col">
                        <h1 className="font-serif text-xl font-semibold">Quản trị</h1>
                        <p className="text-xs text-muted">{isRoot ? "Đăng nhập với quyền quản trị cao nhất" : "Đăng nhập với quyền quản trị viên"}</p>
                    </div>
                    <div className="flex items-center gap-2">
                        <nav aria-label="Liên kết nhanh" className="flex items-center gap-4 text-sm">
                            <Link href="/security/2fa" className="text-primary underline-offset-4 hover:underline">
                                Xác thực hai lớp
                            </Link>
                            <Link href="/chat" className="text-muted underline-offset-4 hover:text-ink hover:underline">
                                Về trang chat
                            </Link>
                        </nav>
                        <ThemeToggle />
                    </div>
                </div>

                <div
                    role="tablist"
                    aria-label="Mục quản trị"
                    className="mx-auto flex w-full max-w-6xl gap-1 overflow-x-auto px-4 pt-4 sm:px-8"
                >
                    {tabs.map((item) => {
                        const active = tab === item.id;
                        return (
                            <button
                                key={item.id}
                                type="button"
                                role="tab"
                                id={`tab-${item.id}`}
                                aria-selected={active}
                                aria-controls={`panel-${item.id}`}
                                onClick={() => setTab(item.id)}
                                className={`whitespace-nowrap border-b-2 px-3 pb-3 text-sm transition-colors ${
                                    active
                                        ? "border-primary font-semibold text-ink"
                                        : "border-transparent text-muted hover:text-ink"
                                }`}
                            >
                                {item.label}
                            </button>
                        );
                    })}
                </div>
            </header>

            <main
                role="tabpanel"
                id={`panel-${tab}`}
                aria-labelledby={`tab-${tab}`}
                className="mx-auto w-full max-w-6xl flex-1 px-4 py-6 sm:px-8"
            >
                {tab === "reports" ? <ReportsTab /> : null}
                {tab === "users" ? <UsersTab isRoot={isRoot} /> : null}
                {tab === "academic" ? <AcademicTab /> : null}
                {tab === "roles" && isRoot ? <RolesTab /> : null}
                {tab === "permissions" && isRoot ? <PermissionsTab /> : null}
                {tab === "audit" ? <AuditTab /> : null}
            </main>
        </div>
    );
}