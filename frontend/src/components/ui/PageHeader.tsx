import type { ReactNode } from "react";

type PageHeaderProps = {
    title: string;
    description?: ReactNode;
    /** Nút hoặc liên kết hành động, đặt bên phải trên màn hình rộng */
    actions?: ReactNode;
};

/** Tiêu đề trang: một cấp h1, mô tả ngắn, hành động xếp cùng hàng. */
export function PageHeader({ title, description, actions }: PageHeaderProps) {
    return (
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
            <div className="flex min-w-0 flex-col gap-1">
                <h1 className="text-2xl font-semibold tracking-tight text-ink text-balance">{title}</h1>
                {description ? <div className="text-sm leading-relaxed text-muted">{description}</div> : null}
            </div>
            {actions ? <div className="flex flex-wrap items-center gap-2">{actions}</div> : null}
        </div>
    );
}
