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
};

export type ChatParams = {
  id: string;
  type: string;
  createdAt: number;
  lastMessage: MessageParams;
  user: UserParams;
  avatarAvailable: boolean;
};
