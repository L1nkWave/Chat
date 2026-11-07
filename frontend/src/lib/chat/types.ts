export type ConversationKind = "direct" | "group";

export type Conversation = {
    id: string;
    title: string;
    kind: ConversationKind;
    lastMessage: string;
    lastTime: string;
    unread: number;
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
};

export type Invitation = {
    id: string;
    groupId: string;
    groupTitle: string;
    inviter: string;
};

export type Member = {
    userId: string;
    name: string;
    isOwner: boolean;
};