"use client";

import { useState } from "react";

import { BTN_GHOST, BTN_PRIMARY } from "./shared";
import { SECTION_TITLE } from "@/components/ui/styles";
import { RoleCreateForm } from "./RoleCreateForm";
import { RoleOverview } from "./RoleOverview";

/** Chỉ hiển thị cho ROOT. Máy chủ vẫn kiểm tra lại quyền ở mọi thao tác. */
export function RolesTab() {
    const [creating, setCreating] = useState(false);
    const [refreshKey, setRefreshKey] = useState(0);

    return (
        <section aria-label="Quản lý vai trò" className="flex flex-col gap-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
                <h2 className={SECTION_TITLE}>Vai trò</h2>
                <button
                    type="button"
                    onClick={() => setCreating((open) => !open)}
                    className={creating ? BTN_GHOST : BTN_PRIMARY}
                >
                    {creating ? "Đóng" : "Thêm vai trò"}
                </button>
            </div>

            {creating ? (
                <RoleCreateForm onCreated={() => setRefreshKey((key) => key + 1)} />
            ) : null}

            <RoleOverview refreshKey={refreshKey} />
        </section>
    );
}