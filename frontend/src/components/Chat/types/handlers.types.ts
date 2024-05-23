import { ChatParams, ContactParams, UserParams } from "@/api/http/contacts/contacts.types";

export type ChatClickHandler = (currentChat: ChatParams) => void;
export type UserClickHandler = (user: UserParams) => void;
export type ContactClickHandler = (contact: ContactParams) => void;
export type SendMessageClickHandler = (author: UserParams, message: string) => void;
