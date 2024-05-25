import React from "react";

import { getChatByUserId } from "@/api/http/chat/chat";
import { ChatParams, ContactParams, MessageParams, UserParams } from "@/api/http/contacts/contacts.types";
import { getUserById } from "@/api/http/user/user";
import { ChatMap, ContactsMap, MessagesMap } from "@/components/Chat/InteractiveList/interactiveList.types";
import {
  BindMessage,
  MessageLikeMessage,
  OnlineOfflineMessage,
  ReadMessage,
  UnreadMessagesMessage,
} from "@/context/SocketContext/socketContext.types";
import { lastSeenDateNow } from "@/helpers/contactHelpers";
import {readMessages} from "@/api/socket";

export const offlineHandler = async (
  socketMessage: OnlineOfflineMessage,
  setChats: React.Dispatch<React.SetStateAction<ChatMap>>,
  contact: ContactParams | undefined,
  setContact: React.Dispatch<React.SetStateAction<ContactParams | undefined>>,
  setContacts: React.Dispatch<React.SetStateAction<ContactsMap>>
) => {
  const socketMessageData = socketMessage;

  if (socketMessageData.senderId === contact?.user.id) {
    setContact(prevContact => ({
      ...prevContact,
      user: {
        ...prevContact!.user,
        online: false,
        lastSeen: lastSeenDateNow(),
      },
    }));
  }
  setContacts(prevContacts => {
    const updatedContact = prevContacts.get(socketMessageData.senderId);
    if (updatedContact) {
      updatedContact.user.online = false;
      updatedContact.user.lastSeen = lastSeenDateNow();
      return new Map(prevContacts).set(socketMessageData.senderId, updatedContact);
    }
    return prevContacts;
  });

  const chatId = await getChatByUserId(socketMessage.senderId.toString());
  setChats(prevChats => {
    const updatedChats = new Map(prevChats);
    const chat = prevChats.get(chatId);
    if (chat) {
      updatedChats.set(chat.id, {
        ...chat,
        user: {
          ...chat.user,
          online: false,
        },
      });
    }
    return updatedChats;
  });
};

export const onlineHandler = async (
  socketMessage: OnlineOfflineMessage,
  setChats: React.Dispatch<React.SetStateAction<ChatMap>>,
  contact: ContactParams | undefined,
  setContact: React.Dispatch<React.SetStateAction<ContactParams | undefined>>,
  setContacts: React.Dispatch<React.SetStateAction<ContactsMap>>
) => {
  if (socketMessage.senderId === contact?.user.id) {
    setContact(prevContact => ({
      ...prevContact,
      user: {
        ...prevContact!.user,
        online: true,
        lastSeen: lastSeenDateNow(),
      },
    }));
  }

  setContacts(prevContacts => {
    const updatedContact = prevContacts.get(socketMessage.senderId);
    if (updatedContact) {
      updatedContact.user.online = true;
      return new Map(prevContacts).set(socketMessage.senderId, updatedContact);
    }
    return prevContacts;
  });

  const chatId = await getChatByUserId(socketMessage.senderId.toString());
  setChats(prevChats => {
    const updatedChats = new Map(prevChats);
    const chat = prevChats.get(chatId);
    if (chatId && chat) {
      updatedChats.set(chatId, {
        ...chat,
        user: {
          ...chat.user,
          online: true,
        },
      });
    }
    return updatedChats;
  });
};

export const bindSocketHandlers = (
  socketMessage: BindMessage,
  currentMessages: MessagesMap,
  setCurrentMessages: React.Dispatch<React.SetStateAction<MessagesMap>>
) => {
  const tempMessage = currentMessages.get(socketMessage.tmpMessageId);
  if (!tempMessage) {
    return;
  }

  setCurrentMessages(prevMessages => {
    prevMessages.delete(tempMessage.id);

    const updatedMessages = new Map<string, MessageParams>();
    updatedMessages.set(socketMessage.messageId, {
      ...tempMessage,
      sending: false,
      id: socketMessage.messageId,
    });
    prevMessages.forEach((value, key) => updatedMessages.set(key, value));

    return updatedMessages;
  });
};

export const messageHandler = async (
  socketMessage: MessageLikeMessage,
  webSocket: WebSocket | null | undefined,
  chatId: string | undefined,
  currentMessages: MessagesMap,
  currentChats: ChatMap,
  fetchChats: () => Promise<void>,
  currentUser: UserParams | null,
  setChats: React.Dispatch<React.SetStateAction<ChatMap>>,
  setCurrentMessages: React.Dispatch<React.SetStateAction<MessagesMap>>
) => {
  if (currentUser && socketMessage.senderId !== currentUser.id.toString() && webSocket && chatId === socketMessage.chatId) {
    readMessages(webSocket, socketMessage.chatId, Date.now() / 1000);
  }

  const author = await getUserById(socketMessage.senderId);
  const messageId = socketMessage.id;
  const message: MessageParams = {
    action: socketMessage.action,
    createdAt: socketMessage.timestamp,
    edited: false,
    isRead: false,
    reactions: [],
    id: messageId,
    text: `${socketMessage.text}`,
    author,
  };

  setChats(prevChat => {
    let updatedChats = new Map(prevChat);
    const currentChat = prevChat.get(socketMessage.chatId);
    if (currentChat) {
      updatedChats.set(socketMessage.chatId, {
        ...currentChat,
        lastMessage: message,
        createdAt: Date.now() / 1000,
      });
    } else {
      updatedChats.set(socketMessage.chatId, {
        id: socketMessage.chatId,
        lastMessage: message,
        createdAt: Date.now() / 1000,
        user: author,
        unreadMessages: 0,
        avatarAvailable: false,
      });
    }
    updatedChats = new Map<string, ChatParams>(
      Array.from(updatedChats).sort((a, b) => b[1].lastMessage.createdAt - a[1].lastMessage.createdAt)
    );
    return updatedChats;
  });

  if (chatId !== socketMessage.chatId) {
    return;
  }


  if (currentMessages.has(messageId)) {
    return;
  }

  if (!currentChats.has(socketMessage.chatId)) {
    await fetchChats();
  }
  setCurrentMessages(prevMessages => {
    const updatedMessages = new Map<string, MessageParams>();
    updatedMessages.set(messageId, {
      ...message,
    });
    prevMessages.forEach((value, key) => updatedMessages.set(key, value));
    return updatedMessages;
  });
};

export const unreadMessagesHandler = (
  socketMessage: UnreadMessagesMessage,
  setChats: React.Dispatch<React.SetStateAction<ChatMap>>
) => {
  setChats(prevChats => {
    const updatedChats = new Map(prevChats);
    Object.entries(socketMessage.chats).forEach(([chatId, unreadMessages]) => {
      const currentChat = prevChats.get(chatId);
      if (currentChat) {
        updatedChats.set(chatId, {
          ...currentChat,
          unreadMessages,
        });
      }
    });
    return updatedChats;
  });
};

export const readMessagesHandler = (
  socketMessage: ReadMessage,
  setChats: React.Dispatch<React.SetStateAction<ChatMap>>,
  setMessages: React.Dispatch<React.SetStateAction<MessagesMap>>
) => {
  setChats(prevChats => {
    const updatedChats = new Map(prevChats);
    const chat = prevChats.get(socketMessage.chatId);
    if (chat) {
      updatedChats.set(socketMessage.chatId, {
        ...chat,
        unreadMessages: 0,
      });
    }
    return updatedChats;
  });

  setMessages(prevMessages => {
    const updatedMessages = new Map(prevMessages);
    socketMessage.messages.forEach(messageId => {
      const message = prevMessages.get(messageId);
      if (message) {
        updatedMessages.set(messageId, {
          ...message,
          isRead: true,
        });
      }
    });
    return updatedMessages;
  });
};
