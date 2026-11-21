import type { ReportCategory } from "@/lib/admin/api";
import { buttonStyle, CARD as KIT_CARD, FIELD, LABEL as KIT_LABEL, SECTION_TITLE } from "@/components/ui/styles";

/* Tên cũ giữ nguyên để các tab quản trị dùng chung một bộ class từ components/ui/styles.ts */
export const INPUT = FIELD;
export const BTN_PRIMARY = buttonStyle("primary", "md");
export const BTN_GHOST = buttonStyle("secondary", "md");
export const LABEL = KIT_LABEL;
export const CARD = KIT_CARD;
export const TITLE = SECTION_TITLE;

export const CATEGORY_LABEL: Record<ReportCategory, string> = {
    HARASSMENT: "Quấy rối",
    PROFANITY: "Ngôn từ thô tục",
    SPAM: "Spam",
    INAPPROPRIATE: "Nội dung không phù hợp",
    OTHER: "Khác",
};

/**
 * Quyền đang dùng trong hệ thống, kèm tên và giải thích bằng tiếng Việt.
 * Mã kỹ thuật (ví dụ announce.send) chỉ dùng trong máy chủ, không hiện cho người dùng.
 */
export const PERMISSION_OPTIONS: { code: string; label: string; description: string }[] = [
    {
        code: "user.view",
        label: "Xem thông tin người dùng",
        description: "Xem hồ sơ, quyền và hoạt động của mọi người dùng trong toàn trường.",
    },
    {
        code: "user.lock",
        label: "Khóa tài khoản",
        description: "Khóa, mở khóa hoặc đặt lại mật khẩu của người có cấp thấp hơn mình.",
    },
    {
        code: "user.role.assign",
        label: "Cấp và thu hồi vai trò",
        description: "Gán hoặc gỡ vai trò cho người dùng, theo phạm vi toàn trường, khoa hoặc lớp.",
    },
    {
        code: "group.view",
        label: "Xem nhóm chat",
        description: "Xem các nhóm chat trong phạm vi được giao (lớp, môn, khoa).",
    },
    {
        code: "group.manage",
        label: "Quản lý nhóm chat",
        description: "Thêm, bớt thành viên và quản lý nhóm chat trong phạm vi được giao.",
    },
    {
        code: "announce.send",
        label: "Nhắn thông báo",
        description: "Gửi thông báo tới toàn bộ nhóm hoặc lớp trong phạm vi được giao.",
    },
    {
        code: "moderation.view",
        label: "Xem tin nhắn bị báo cáo",
        description: "Xem danh sách báo cáo tin nhắn đang chờ xử lý.",
    },
    {
        code: "moderation.resolve",
        label: "Xử lý báo cáo tin nhắn",
        description: "Bỏ qua, cảnh báo, hoặc gỡ tin nhắn vi phạm và cảnh báo người gửi.",
    },
    {
        code: "audit.view",
        label: "Xem nhật ký hệ thống",
        description: "Xem nhật ký hoạt động: ai làm gì, với đối tượng nào, lúc nào và từ IP nào.",
    },
    {
        code: "import.run",
        label: "Nhập dữ liệu từ Excel",
        description: "Nhập khoa, lớp, môn học, giảng viên, sinh viên, thời khóa biểu, lịch thi và học kỳ.",
    },
];

/** Nhãn của một mã quyền; nếu chưa có trong danh mục thì trả về chính mã */
export function permissionLabel(code: string): string {
    return PERMISSION_OPTIONS.find((item) => item.code === code)?.label ?? code;
}