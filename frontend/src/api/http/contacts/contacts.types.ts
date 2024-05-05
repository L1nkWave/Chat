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
  alias?: string;
  addedAt?: string;
  user: UserParams;
};

export type MessageParams = {
  action: string;
  author: UserParams;
  createdAt: string;
  edited: boolean;
  id: string;
  isRead: boolean;
  reactions: string[];
  text: string;
};

export type ChatParams = {
  createdAt: string;
  id: string;
  lastMessage: MessageParams;
  type: number;
};
