"use client";

import React, { useCallback, useContext, useState } from "react";
import { toast } from "react-toastify";
import { v4 as uuidv4 } from "uuid";

import { addDuoChat, AddDuoChatParams, getChatByUserId, getChats, getMessagesByChatId } from "@/api/http/chat/chat";
import { addContact, getContacts, removeContact } from "@/api/http/contacts/contacts";
import { ChatParams, ContactParams, UserParams } from "@/api/http/contacts/contacts.types";
import { getUserById, searchUser } from "@/api/http/user/user";
import { checkUnreadMessages, readMessages, sendChatMessage } from "@/api/socket";
import {
  bindSocketHandlers,
  messageHandler,
  offlineHandler,
  onlineHandler,
  readMessagesHandler,
  unreadMessagesHandler,
} from "@/components/Chat/chat.socketHandlers";
import { ListStateEnum, MainBoxStateEnum } from "@/components/Chat/chat.types";
import { InteractiveList } from "@/components/Chat/InteractiveList/InteractiveList";
import { ChatMap, ContactsMap, MessagesMap, UserMap } from "@/components/Chat/InteractiveList/interactiveList.types";
import { MainBox } from "@/components/Chat/MainBox/MainBox";
import { SideBar } from "@/components/Chat/SideBar/SideBar";
import { SIDEBAR_ITEM } from "@/components/Chat/SideBar/sidebar.config";
import { SidebarButtonName } from "@/components/Chat/SideBar/sidebar.types";
import { CustomButton } from "@/components/CustomButton/CustomButton";
import { SocketContext } from "@/context/SocketContext/SocketContext";
import {
  BindMessage,
  MessageAction,
  MessageLikeMessage,
  OnlineOfflineMessage,
  ReadMessage,
  UnreadMessagesMessage,
} from "@/context/SocketContext/socketContext.types";
import { SoundContext } from "@/context/SoundContext/SoundContext";
import { useAccessTokenEffect, useAccessTokenLayoutEffect } from "@/hooks/useAccessTokenEffect";
import { setCurrentInteractiveListState, setCurrentMainBoxState } from "@/lib/features/chat/chatSlice";
import { useAppDispatch, useAppSelector } from "@/lib/hooks";

export function Chat() {
  const dispatch = useAppDispatch();

  const { messageSound } = useContext(SoundContext);
  const { webSocket } = useContext(SocketContext);
  const { currentUser } = useAppSelector(state => state.user);
  const { currentMainBoxState, currentInteractiveListState } = useAppSelector(state => state.chat);
  const { socketMessage } = useContext(SocketContext);

  const [contacts, setContacts] = useState<ContactsMap>(new Map());
  const [contact, setContact] = useState<ContactParams | undefined>(undefined);

  const [globalUsers, setGlobalUsers] = useState<UserMap>(new Map());
  const [globalUser, setGlobalUser] = useState<UserParams | undefined>(undefined);

  const [chats, setChats] = useState<ChatMap>(new Map());
  const [chatId, setChatId] = useState<string | undefined>(undefined);
  const [currentMessages, setCurrentMessages] = useState<MessagesMap>(new Map());

  const [currentSidebarItem, setCurrentSidebarItem] = useState<string>("chat" as SidebarButtonName);

  const fetchGlobalContacts = useCallback(async (search?: string, offset?: number, limit?: number) => {
    try {
      const fetchedGlobalUsers: Map<string, UserParams> = await searchUser(search, limit, offset);
      setGlobalUsers(prevGlobalUsers => {
        const updatedUsers = new Map(prevGlobalUsers);
        fetchedGlobalUsers.forEach((user, key) => {
          updatedUsers.set(key, user);
        });
        return updatedUsers;
      });
    } catch (error) {
      toast.error("Error fetching global users");
    }
  }, []);

  const fetchContacts = useCallback(async (search?: string, offset?: number, limit?: number) => {
    try {
      const fetchedContacts = await getContacts(search, limit, offset);
      setContacts(prevContacts => {
        const updatedContacts = new Map(prevContacts);
        fetchedContacts.forEach((fetchedContact, key) => {
          updatedContacts.set(key, fetchedContact);
        });
        return updatedContacts;
      });
    } catch (error) {
      toast.error("Error fetching contacts");
    }
  }, []);

  const fetchChats = useCallback(async (offset?: number, limit?: number) => {
    try {
      const fetchedChats = await getChats(offset, limit);
      setChats(prevChats => {
        const updatedChats = new Map(prevChats);
        fetchedChats.forEach((fetchedChat, key) => {
          updatedChats.set(key, fetchedChat);
        });
        return updatedChats;
      });
    } catch (error) {
      toast.error("Error fetching chats");
    }
  }, []);

  useAccessTokenEffect(() => {
    fetchContacts();
  }, []);

  useAccessTokenEffect(() => {
    fetchGlobalContacts();
  }, []);

  useAccessTokenEffect(() => {
    fetchChats();
  }, []);

  useAccessTokenEffect(() => {
    if (currentMainBoxState !== MainBoxStateEnum.CHAT) {
      setChatId(undefined);
    }
  }, [currentMainBoxState]);

  useAccessTokenEffect(() => {
    if (chatId && webSocket) {
      readMessages(webSocket, chatId, Date.now() / 1000);
    }
  }, [chatId]);

  useAccessTokenEffect(() => {
    if (webSocket && webSocket.readyState === WebSocket.OPEN) {
      setTimeout(() => {
        checkUnreadMessages(webSocket);
      }, 300);
    }
  }, [webSocket?.readyState, fetchChats]);

  useAccessTokenLayoutEffect(() => {
    if (!socketMessage) {
      return;
    }
    if (socketMessage.action === MessageAction.OFFLINE) {
      offlineHandler(socketMessage as OnlineOfflineMessage, setChats, contact, setContact, setContacts);
    } else if (socketMessage.action === MessageAction.ONLINE) {
      onlineHandler(socketMessage as OnlineOfflineMessage, setChats, contact, setContact, setContacts);
    } else if (socketMessage.action === MessageAction.BIND) {
      bindSocketHandlers(socketMessage as BindMessage, currentMessages, setCurrentMessages);
    } else if (socketMessage.action === MessageAction.MESSAGE) {
      messageHandler(
        socketMessage as MessageLikeMessage,
        webSocket,
        chatId,
        currentMessages,
        chats,
        fetchChats,
        currentUser,
        setChats,
        setCurrentMessages
      );
      checkUnreadMessages(webSocket as WebSocket);
      if (chatId !== (socketMessage as MessageLikeMessage).chatId) {
        messageSound();
      }
    } else if (socketMessage.action === MessageAction.UNREAD_MESSAGES) {
      unreadMessagesHandler(socketMessage as UnreadMessagesMessage, setChats);
    } else if (socketMessage.action === MessageAction.READ) {
      readMessagesHandler(socketMessage as ReadMessage, setChats, setCurrentMessages);
      checkUnreadMessages(webSocket as WebSocket);
    }
  }, [socketMessage]);

  const handleContactClick = async (currentContact: ContactParams) => {
    setGlobalUser(undefined);
    const fetchedUser = await getUserById(currentContact.user.id.toString());
    setContact({
      ...currentContact,
      user: fetchedUser,
    });
    dispatch(setCurrentMainBoxState(MainBoxStateEnum.USER_INFO));
  };

  const handleGlobalContactClick = (currentGlobalUser: UserParams) => {
    setContact(undefined);
    setGlobalUser(currentGlobalUser);
    dispatch(setCurrentMainBoxState(MainBoxStateEnum.USER_INFO));
  };

  const handleAddContact = async (userId: string, alias: string) => {
    const addDuoChatWithTryCatch = async () => {
      try {
        await addDuoChat(userId);
      } catch (error) {
        // TODO: catch this somehow!
      }
    };

    try {
      await addContact(userId, alias);
      await addDuoChatWithTryCatch();

      setGlobalUsers(prevGlobalUsers => {
        const updatedUsers = new Map(prevGlobalUsers);
        updatedUsers.delete(userId);
        return updatedUsers;
      });
      await fetchContacts();

      if (globalUser) {
        setContact(contacts.get(globalUser.id));
      }
      setGlobalUser(undefined);

      await fetchChats();
    } catch (error) {
      toast.error("Error adding contact");
    }
  };

  const handleRemoveContact = async (userId: string) => {
    try {
      await removeContact(userId);

      setContacts(prevContacts => {
        const updatedContacts = new Map(prevContacts);
        updatedContacts.delete(userId);
        return updatedContacts;
      });

      if (contact) {
        setGlobalUser(contact.user);
      }
      setContact(undefined);

      await fetchGlobalContacts();
      await fetchChats();
    } catch (error) {
      toast.error("Error adding contact");
    }
  };

  const handleSendMessageButtonClick = async (author: UserParams, chatMessage: string) => {
    if (webSocket && chatId) {
      const tempId = uuidv4();
      const newMessage = {
        id: tempId,
        text: chatMessage,
        author,
        createdAt: Date.now() / 1000,
        action: MessageAction.MESSAGE,
        edited: false,
        isRead: false,
        sending: true,
        reactions: [],
      };
      setCurrentMessages(prevMessages => {
        const updatedMessages = new Map();
        updatedMessages.set(tempId, newMessage);
        prevMessages.forEach((message, key) => updatedMessages.set(key, message));
        return updatedMessages;
      });

      setChats(prevChat => {
        let updatedChats = new Map(prevChat);
        const currentChat = prevChat.get(chatId);
        if (currentChat) {
          updatedChats.set(chatId, {
            ...currentChat,
            lastMessage: newMessage,
            createdAt: Date.now() / 1000,
          });
        }
        updatedChats = new Map<string, ChatParams>(
          Array.from(updatedChats).sort((a, b) => b[1].lastMessage.createdAt - a[1].lastMessage.createdAt)
        );
        return updatedChats;
      });
      sendChatMessage(webSocket, chatId, chatMessage, tempId);
    }
  };

  const handleMessageButtonClick = async (userId: string) => {
    let newChatId: AddDuoChatParams;
    try {
      newChatId = {
        id: await getChatByUserId(userId),
        createdAt: Date.now() / 1000,
      };
    } catch (error) {
      newChatId = await addDuoChat(userId);
    }
    if (!contact && globalUser) {
      setContact({
        addedAt: undefined,
        alias: globalUser.name,
        user: globalUser,
      });
      setGlobalUser(undefined);
    }

    setChatId(newChatId.id);
    setCurrentMessages(await getMessagesByChatId(newChatId.id));
    if (webSocket && webSocket.readyState === WebSocket.OPEN) {
      setTimeout(() => {
        checkUnreadMessages(webSocket);
      }, 300);
    }
    dispatch(setCurrentMainBoxState(MainBoxStateEnum.CHAT));
  };

  const handleChatClick = async (currentChat: ChatParams) => {
    setContact({
      addedAt: undefined,
      alias: currentChat.user.name,
      user: currentChat.user,
    });
    setChatId(currentChat.id);
    setCurrentMessages(new Map());
    dispatch(setCurrentMainBoxState(MainBoxStateEnum.CHAT));
    setCurrentMessages(await getMessagesByChatId(currentChat.id));
  };

  SIDEBAR_ITEM.buttons[ListStateEnum.CONTACTS].onClick = () => {
    dispatch(setCurrentInteractiveListState(ListStateEnum.CONTACTS));
  };

  SIDEBAR_ITEM.buttons[ListStateEnum.CHATS].onClick = () => {
    dispatch(setCurrentInteractiveListState(ListStateEnum.CHATS));
  };

  SIDEBAR_ITEM.buttons[ListStateEnum.FIND_CONTACTS].onClick = () => {
    dispatch(setCurrentInteractiveListState(ListStateEnum.FIND_CONTACTS));
  };

  SIDEBAR_ITEM.buttons["add-chat"].onClick = () => {
    console.log("add-chat");
  };

  SIDEBAR_ITEM.buttons.setting.onClick = () => {
    console.log("setting");
  };

  const loadGlobalUsers = async (search?: string, offset?: number) => {
    await fetchGlobalContacts(search, offset);
  };
  const loadChats = async (offset?: number) => {
    await fetchChats(offset);
  };
  const loadContacts = async (search?: string, offset?: number) => {
    await fetchContacts(search, offset);
  };
  const loadMessages = async (offset?: number) => {
    if (chatId) {
      const newMessages = await getMessagesByChatId(chatId, offset);

      setCurrentMessages(prevMessages => {
        const updatedMessages = new Map(prevMessages);
        newMessages.forEach((message, key) => {
          updatedMessages.set(key, message);
        });
        return updatedMessages;
      });
    }
  };

  return (
    <div className="w-screen flex px-64">
      <SideBar>
        {Object.entries(SIDEBAR_ITEM.buttons).map(([buttonName, buttonParams]) => (
          <CustomButton
            key={buttonName}
            className={currentSidebarItem === buttonName ? "dark:bg-dark-50" : ""}
            onClick={() => {
              setCurrentSidebarItem(buttonName);
              if (buttonParams.onClick) {
                buttonParams.onClick();
              }
            }}
            variant={SIDEBAR_ITEM.variant}
            icon={buttonParams.icon}
            iconSize={SIDEBAR_ITEM.iconSize}
          />
        ))}
      </SideBar>
      <InteractiveList
        interactiveListVariant={currentInteractiveListState}
        interactiveContact={{
          currentContact: contact,
          contacts,
          onContactClick: handleContactClick,
          loadContacts,
        }}
        interactiveChat={{
          chats,
          onChatClick: handleChatClick,
          loadChats,
        }}
        interactiveFindContacts={{
          currentGlobalUser: globalUser,
          globalContacts: globalUsers,
          loadGlobalContacts: loadGlobalUsers,
          onGlobalContactClick: handleGlobalContactClick,
        }}
      />
      <MainBox
        chatId={chatId}
        mainBoxVariant={currentMainBoxState}
        contact={contact}
        globalUser={globalUser}
        messages={Array.from(currentMessages.values())}
        onAddContactClick={handleAddContact}
        onRemoveContactClick={handleRemoveContact}
        onMessageButtonClick={handleMessageButtonClick}
        onSendMessageClick={handleSendMessageButtonClick}
        onHeaderClick={handleContactClick}
        loadMessages={loadMessages}
      />
    </div>
  );
}
