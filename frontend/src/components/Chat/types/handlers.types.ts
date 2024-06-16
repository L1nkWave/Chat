import { ChatParams, ContactParams, UserParams } from "@/api/http/contacts/contacts.types";

export type ChatClickHandler = (currentChat: ChatParams) => void;
export type UserClickHandler = (user: UserParams) => void;
export type ContactClickHandler = (contact: ContactParams) => void;
export type SendMessageClickHandler = (author: UserParams, message: string, file: File | null) => void;
export type LoadContactsHandler = (search?: string, offset?: number) => void;
export type LoadChatsHandler = (offset?: number) => void;
export type LoadMessagesHandler = (offset?: number) => void;
