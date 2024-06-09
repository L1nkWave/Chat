import { ChatType } from "@/api/socket/index.types";

export type UserParams = {
  id: number;
  bio?: string;
  name: string;
  online: boolean;
  lastSeen: number;
  username: string;
  createdAt: string;
  avatarPath?: string;
  deleted?: boolean;
};

export type ContactParams = {
  alias?: string;
  addedAt?: string;
  user: UserParams;
};

export type MessageParams = {
  action: string;
  author: UserParams;
  createdAt: number;
  edited: boolean;
  id: string;
  isRead: boolean;
  reactions: string[];
  text: string;
  sending?: boolean;
  contentType?: string;
  size?: number;
  filename?: string;
};

export type ChatParams = {
  id: string;
  type: ChatType;
  name: string;
  avatarAvailable?: boolean;
  createdAt: number;
  lastMessage?: MessageParams;
  unreadMessages: number;
  user: UserParams;
};

export type GroupMemberDetails = {
  username: string;
  name: string;
  avatarPath: null | string;
  online: boolean;
};

export enum GroupRole {
  ADMIN = "ADMIN",
  MEMBER = "MEMBER",
}

export type GroupMember = {
  id: string;
  role: GroupRole;
  joinedAt: number;
  details: GroupMemberDetails;
};

export type GroupChatDetails = {
  description: string;
  members: Map<string, GroupMember>;
  membersLimit: number;
  private: boolean;
};

export type FileHttpResponse = {
  id: string;
  createdAt: number;
  filename: string;
  contentType: string;
  size: number;
};
