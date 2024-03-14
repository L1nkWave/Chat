export type ContactParams = {
  id: string;
  name: string;
  avatar: string;
  active: boolean;
};

export type ChatParams = {
  id: string;
  name: string;
  avatar: string;
  lastMessage: string;
  unreadMessagesCount: number;
};
