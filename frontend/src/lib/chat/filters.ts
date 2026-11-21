import type { ChatCategory, Conversation } from "./types";

/** Bộ lọc trên thanh sidebar. "ALL" là không lọc. */
export type CategoryFilter = "ALL" | ChatCategory;

export const CATEGORY_FILTERS: { value: CategoryFilter; label: string }[] = [
    { value: "ALL", label: "Tất cả" },
    { value: "PRIVATE", label: "Riêng tư" },
    { value: "CLASS", label: "Lớp" },
    { value: "SUBJECT", label: "Môn" },
    { value: "FACULTY", label: "Khoa" },
    { value: "GROUP", label: "Nhóm" },
];

export const CATEGORY_LABEL: Record<ChatCategory, string> = {
    PRIVATE: "Riêng tư",
    CLASS: "Lớp",
    SUBJECT: "Môn",
    FACULTY: "Khoa",
    GROUP: "Nhóm",
};

export function matchesCategory(conversation: Conversation, filter: CategoryFilter): boolean {
    return filter === "ALL" || conversation.category === filter;
}
