import type { Conversation } from "@/lib/chat/types";

/** Tài khoản bot hệ thống (username trong bảng users). */
export const BOT_USERNAME = "sgu_bot";

/**
 * Chuyển thời gian từ server sang mili giây.
 * Java có thể gửi phần thập phân nhiều hơn 3 chữ số; chỉ giữ mili giây để so sánh an toàn.
 */
export function timeValue(value: string): number {
    const t = Date.parse(value.replace(/(\.\d{3})\d+/, "$1"));
    return Number.isNaN(t) ? 0 : t;
}

/**
 * Thứ tự danh sách: bot hệ thống đứng đầu, rồi các chat đã ghim (ghim mới nhất trước),
 * rồi các chat còn lại theo thời gian tin nhắn cuối (mới nhất trước). Không sắp theo tên.
 */
export function sortConversations(list: Conversation[]): Conversation[] {
    return [...list].sort((a, b) => {
        if (a.isBot !== b.isBot) return a.isBot ? -1 : 1;
        if (a.pinned !== b.pinned) return a.pinned ? -1 : 1;
        if (a.pinned && b.pinned) {
            const byPin = timeValue(b.pinnedAt ?? "") - timeValue(a.pinnedAt ?? "");
            if (byPin !== 0) return byPin;
        }
        return timeValue(b.lastTime) - timeValue(a.lastTime);
    });
}
