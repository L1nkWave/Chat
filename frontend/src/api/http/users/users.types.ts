export type UserParams = {
  id: number;
  bio?: string;
  name: string;
  online: boolean;
  lastSeen: number;
  username: string;
  createdAt: string;
  avatarPath?: string;
};

export type ContactParams = {
  alias: string;
  addedAt: string;
  user: UserParams;
};

export type ChatParams = {
  id: number;
  name: string;
  avatar: string;
  lastMessage: string;
  unreadMessagesCount: number;
};
