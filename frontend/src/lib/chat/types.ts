export type ConversationKind = "direct" | "group";

/** Loại cuộc trò chuyện do server phân loại */
export type ChatCategory = "PRIVATE" | "GROUP" | "CLASS" | "SUBJECT" | "FACULTY";

export type Conversation = {
    id: string;
    title: string;
    kind: ConversationKind;
    category: ChatCategory;
    lastMessage: string;
    lastTime: string;
    unread: number;
    /** Đã ghim (theo người đang đăng nhập). Chat bot luôn ở mục ghim nhưng không đổi được. */
    pinned: boolean;
    /** Thời điểm ghim, dùng để sắp xếp các chat đã ghim (ghim gần nhất đứng trước) */
    pinnedAt: string | null;
    /** Chat với tài khoản bot hệ thống: không ghim/bỏ ghim và không ẩn được */
    isBot: boolean;
};

export type Attachment = {
    id: string;
    filename: string;
    contentType: string;
    size: number;
};

export type Message = {
    id: string;
    conversationId: string;
    senderId: string;
    senderName: string;
    text: string;
    time: string;
    attachment?: Attachment | null;
    edited?: boolean;
    editedAt?: string;
    removedAt?: string;
};

/** Một ảnh hoặc tệp trong kho phương tiện của cuộc trò chuyện */
export type MediaItem = {
    messageId: string;
    filename: string;
    contentType: string;
    size: number;
    createdAt: string;
    authorId: number | string;
    removedAt: string | null;
};

export type Invitation = {
    id: string;
    groupId: string;
    groupTitle: string;
    inviter: string;
};

/** Người dùng gợi ý khi tìm để nhắn riêng */
export type UserSuggestion = {
    id: string;
    username: string;
    name: string;
};

export type Member = {
    userId: string;
    name: string;
    isOwner: boolean;
};
