import type { ReportCategory } from "@/lib/admin/api";

export const INPUT =
    "h-10 min-w-0 w-full rounded-md border border-line bg-paper px-3 text-sm text-ink outline-none transition-colors placeholder:text-muted focus:border-primary";
export const BTN_PRIMARY =
    "h-10 shrink-0 rounded-md bg-primary px-4 text-sm font-medium text-primary-ink transition-colors hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-40";
export const BTN_GHOST =
    "h-10 shrink-0 rounded-md border border-line px-4 text-sm text-ink transition-colors hover:bg-tint disabled:cursor-not-allowed disabled:opacity-40";
export const LABEL = "text-xs font-semibold uppercase tracking-wide text-muted";
export const CARD = "flex flex-col gap-3 rounded-lg border border-line bg-surface p-4";

export const CATEGORY_LABEL: Record<ReportCategory, string> = {
    HARASSMENT: "Quấy rối",
    PROFANITY: "Ngôn từ thô tục",
    SPAM: "Spam",
    INAPPROPRIATE: "Nội dung không phù hợp",
    OTHER: "Khác",
};

/** Các quyền đang dùng trong hệ thống, kèm giải thích bằng tiếng Việt. */
export const PERMISSION_OPTIONS: { code: string; label: string; description: string }[] = [
    {
        code: "user.view",
        label: "Xem người dùng",
        description: "Xem hồ sơ và quyền của mọi người dùng trong toàn trường.",
    },
    {
        code: "user.lock",
        label: "Khóa tài khoản",
        description: "Khóa hoặc mở khóa tài khoản của người có cấp thấp hơn mình.",
    },
    {
        code: "audit.view",
        label: "Xem nhật ký",
        description: "Xem nhật ký hoạt động của hệ thống, gồm ai làm gì và từ IP nào.",
    },
    {
        code: "moderation.view",
        label: "Xem báo cáo",
        description: "Xem danh sách báo cáo tin nhắn đang chờ xử lý.",
    },
    {
        code: "moderation.resolve",
        label: "Xử lý báo cáo",
        description: "Bỏ qua, cảnh báo, hoặc gỡ nội dung tin nhắn vi phạm và cảnh báo tác giả.",
    },
];