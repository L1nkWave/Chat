/**
 * Bộ class dùng chung cho toàn bộ giao diện (chat, quản trị, tài khoản, đăng nhập).
 * Mọi màn hình lấy class từ đây, không tự viết lại. Màu chỉ lấy từ token trong globals.css.
 */

/* Ô nhập, dropdown: cùng chiều cao, cùng viền, cùng trạng thái focus */
export const FIELD =
    "h-10 w-full min-w-0 rounded-md border border-line bg-surface px-3 text-sm text-ink outline-none transition-colors placeholder:text-muted focus:border-primary focus-visible:ring-2 focus-visible:ring-primary/25 disabled:opacity-50";

export const FIELD_INVALID = "border-danger focus:border-danger";

export const TEXTAREA = `${FIELD} h-auto min-h-24 py-2 leading-relaxed`;

export const SELECT = `${FIELD} pr-8`;

/* Nút: một kích thước chính (h-10) và một kích thước nhỏ (h-8) cho thanh công cụ và bảng */
const BUTTON_BASE =
    "inline-flex shrink-0 select-none items-center justify-center gap-2 rounded-md font-medium whitespace-nowrap transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary disabled:cursor-not-allowed disabled:opacity-50";

export const BUTTON_SIZE = {
    md: "h-10 px-4 text-sm",
    sm: "h-8 px-3 text-xs",
} as const;

export const BUTTON_VARIANT = {
    primary: "bg-primary text-primary-ink hover:bg-primary-hover",
    secondary: "border border-line bg-surface text-ink hover:bg-tint",
    ghost: "text-primary hover:bg-tint",
    danger: "border border-line bg-surface text-danger hover:border-danger hover:bg-danger/10",
} as const;

export type ButtonVariant = keyof typeof BUTTON_VARIANT;
export type ButtonSize = keyof typeof BUTTON_SIZE;

export function buttonStyle(variant: ButtonVariant = "primary", size: ButtonSize = "md", extra = ""): string {
    return [BUTTON_BASE, BUTTON_SIZE[size], BUTTON_VARIANT[variant], extra].filter(Boolean).join(" ");
}

/* Khối nội dung: nền trắng, viền mảnh, không bóng */
export const CARD = "flex flex-col gap-4 rounded-lg border border-line bg-surface p-5";

/* Tiêu đề khối và nhãn trường. Không dùng chữ in hoa nhỏ làm tiêu đề. */
export const SECTION_TITLE = "text-base font-semibold text-ink";
export const LABEL = "text-sm font-medium text-ink";
export const HELP = "text-[13px] leading-relaxed text-muted";

/* Bảng dữ liệu: số dùng chữ số cố định để các cột thẳng hàng */
export const TABLE_WRAP = "overflow-x-auto rounded-lg border border-line bg-surface";
export const TABLE = "w-full border-collapse text-left text-sm";
export const TH = "whitespace-nowrap border-b border-line bg-paper px-3 py-2.5 text-xs font-semibold text-muted";
export const TD = "border-b border-line px-3 py-3 align-middle text-ink tabular-nums last:border-b-0";

/* Nhãn trạng thái */
const BADGE_BASE =
    "inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium whitespace-nowrap";
export const BADGE_TONE = {
    neutral: "bg-tint text-ink",
    success: "bg-success/12 text-success",
    danger: "bg-danger/10 text-danger",
    outline: "border border-line text-muted",
} as const;
export type BadgeTone = keyof typeof BADGE_TONE;
export function badgeStyle(tone: BadgeTone = "neutral", extra = ""): string {
    return [BADGE_BASE, BADGE_TONE[tone], extra].filter(Boolean).join(" ");
}

/* Liên kết trong nội dung */
export const LINK = "text-primary underline-offset-4 hover:underline focus-visible:outline-primary";

/* Ô trạng thái trống và đang tải */
export const EMPTY = "rounded-lg border border-dashed border-line px-4 py-8 text-center text-sm text-muted";
