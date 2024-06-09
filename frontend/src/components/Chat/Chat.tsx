"use client";

import React, { useCallback, useContext, useState } from "react";
import { toast } from "react-toastify";
import { v4 as uuidv4 } from "uuid";

import {
  addDuoChat,
  AddDuoChatParams,
  createGroupChat,
  getChatByUserId,
  getChats,
  getGroupChatDetailsById,
  getMessagesByChatId,
  sendFile,
} from "@/api/http/chat/chat";
import { addContact, getContacts, removeContact } from "@/api/http/contacts/contacts";
import {
  ChatParams,
  ContactParams,
  GroupChatDetails,
  MessageParams,
  UserParams,
} from "@/api/http/contacts/contacts.types";
import { getUserById, searchUser, uploadAvatar, uploadGroupAvatar } from "@/api/http/user/user";
import {
  addMemberToGroupChat,
  checkUnreadMessages,
  readMessages,
  sendChatMessage,
  sendFileMessage,
} from "@/api/socket";
import { ChatType } from "@/api/socket/index.types";
import {
  addMessagesHandler,
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
import { CreateGroupChatModal } from "@/components/CreateGroupChatModal/CreateGroupChatModal";
import { CustomButton } from "@/components/CustomButton/CustomButton";
import { SocketContext } from "@/context/SocketContext/SocketContext";
import {
  AddMessage,
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
import { setCurrentUser } from "@/lib/features/user/userSlice";
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
  const [chat, setChat] = useState<ChatParams | undefined>(undefined);
  const [currentMessages, setCurrentMessages] = useState<MessagesMap>(new Map());
  const [totalMessages, setTotalMessages] = useState<number>(0);
  const [groupDetails, setGroupDetails] = useState<GroupChatDetails | undefined>(undefined);

  const [currentSidebarItem, setCurrentSidebarItem] = useState<string>("chat" as SidebarButtonName);

  const [isCreateGroupChatModalOpen, setIsCreateGroupChatModalOpen] = useState(false);

  const reFetchGlobalContacts = useCallback(async () => {
    try {
      const fetchedGlobalUsers: UserMap = await searchUser();
      setGlobalUsers(fetchedGlobalUsers);
    } catch (error) {
      toast.error("Error fetching global users");
    }
  }, []);

  const fetchGlobalContacts = useCallback(async (search?: string, offset?: number, limit?: number) => {
    try {
      const fetchedGlobalUsers: UserMap = await searchUser(search, limit, offset);
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

  const reFetchContacts = useCallback(async () => {
    try {
      const fetchedContacts = await getContacts();
      setContacts(fetchedContacts);
    } catch (error) {
      toast.error("Error fetching contacts");
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
      setChat(undefined);
    }
  }, [currentMainBoxState]);

  useAccessTokenEffect(() => {
    if (chat && webSocket) {
      readMessages(webSocket, chat.id, Date.now() / 1000);
    }
  }, [chat]);

  useAccessTokenEffect(() => {
    if (webSocket && webSocket.readyState === WebSocket.OPEN) {
      setTimeout(() => {
        checkUnreadMessages(webSocket);
      }, 300);
    }
  }, [webSocket?.readyState, fetchChats]);

  useAccessTokenLayoutEffect(() => {
    console.log(socketMessage);
    if (!socketMessage) {
      return;
    }
    if (socketMessage.action === MessageAction.OFFLINE) {
      offlineHandler(
        socketMessage as OnlineOfflineMessage,
        setChats,
        contact,
        setContact,
        setContacts,
        setGroupDetails
      );
    } else if (socketMessage.action === MessageAction.ONLINE) {
      onlineHandler(socketMessage as OnlineOfflineMessage, setChats, contact, setContact, setContacts, setGroupDetails);
    } else if (socketMessage.action === MessageAction.BIND) {
      bindSocketHandlers(socketMessage as BindMessage, currentMessages, setCurrentMessages);
    } else if (socketMessage.action === MessageAction.MESSAGE || socketMessage.action === MessageAction.FILE) {
      messageHandler(
        socketMessage as MessageLikeMessage,
        webSocket,
        chat?.id,
        currentMessages,
        chats,
        fetchChats,
        currentUser,
        setChats,
        setCurrentMessages
      );
      checkUnreadMessages(webSocket as WebSocket);
      if (chat?.id !== (socketMessage as MessageLikeMessage).chatId) {
        messageSound();
      }
    } else if (socketMessage.action === MessageAction.UNREAD_MESSAGES) {
      unreadMessagesHandler(socketMessage as UnreadMessagesMessage, setChats);
    } else if (socketMessage.action === MessageAction.READ) {
      readMessagesHandler(socketMessage as ReadMessage, setChats, setCurrentMessages);
      checkUnreadMessages(webSocket as WebSocket);
    } else if (socketMessage.action === MessageAction.ADD) {
      addMessagesHandler(socketMessage as AddMessage, setGroupDetails);
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

  const handleAddContact = async (userId: number, alias: string) => {
    const addDuoChatWithTryCatch = async () => {
      try {
        await addDuoChat(userId.toString());
      } catch (error) {
        // TODO: catch this somehow!
      }
    };

    try {
      await addContact(userId.toString(), alias);
      await addDuoChatWithTryCatch();

      setGlobalUsers(prevGlobalUsers => {
        const updatedUsers = new Map(prevGlobalUsers);
        updatedUsers.delete(userId);
        return updatedUsers;
      });
      await reFetchContacts();
      if (globalUser) {
        setContact({
          addedAt: Date.now().toString(),
          alias,
          user: globalUser,
        });
      }
      setGlobalUser(undefined);

      await fetchChats();
    } catch (error) {
      toast.error("Error adding contact");
    }
  };

  const handleRemoveContact = async (userId: number) => {
    try {
      await removeContact(userId.toString());
      setContacts(prevContacts => {
        const updatedContacts = new Map(prevContacts);
        updatedContacts.delete(userId);
        return updatedContacts;
      });

      if (contact) {
        setGlobalUser(contact.user);
      }
      setContact(undefined);

      await reFetchGlobalContacts();
      await fetchChats();
    } catch (error) {
      toast.error("Error adding contact");
    }
  };

  // eslint-disable-next-line sonarjs/cognitive-complexity
  const handleSendMessageButtonClick = async (author: UserParams, chatMessage: string, file: File | null) => {
    if (webSocket && chat) {
      setTotalMessages(prevTotalMessages => prevTotalMessages + 1);
      if (file) {
        const response = await sendFile(chat.id, file);
        sendFileMessage(webSocket, chat.id, response);
      }
      if (chatMessage === "") {
        return;
      }
      const tempId = uuidv4();
      const newMessage: MessageParams = {
        id: tempId,
        text: chatMessage,
        author,
        createdAt: Date.now() / 1000,
        action: MessageAction.MESSAGE,
        edited: false,
        isRead: false,
        sending: true,
        reactions: [],
        contentType: undefined,
        size: undefined,
        filename: undefined,
      };
      setCurrentMessages(prevMessages => {
        const updatedMessages = new Map();
        updatedMessages.set(tempId, newMessage);
        prevMessages.forEach((message, key) => updatedMessages.set(key, message));
        return updatedMessages;
      });

      setChats(prevChat => {
        let updatedChats = new Map(prevChat);
        const currentChat = prevChat.get(chat.id);
        if (currentChat) {
          updatedChats.set(chat.id, {
            ...currentChat,
            lastMessage: newMessage,
            createdAt: Date.now() / 1000,
          });
        }
        if (updatedChats.size > 1) {
          updatedChats = new Map<string, ChatParams>(
            Array.from(updatedChats).sort((a, b) => {
              const first = a[1];
              const second = b[1];

              if (first.lastMessage && second.lastMessage) {
                return second.lastMessage.createdAt - first.lastMessage.createdAt;
              }
              if (first.lastMessage) {
                return second.createdAt - first.lastMessage.createdAt;
              }
              if (second.lastMessage) {
                return second.lastMessage.createdAt - first.createdAt;
              }
              return second.createdAt - first.createdAt;
            })
          );
        }
        return updatedChats;
      });
      sendChatMessage(webSocket, chat.id, chatMessage, tempId);
    }
  };

  const handleMessageButtonClick = async (userId: number) => {
    let newChatId: AddDuoChatParams;
    try {
      newChatId = {
        id: await getChatByUserId(userId.toString()),
        createdAt: Date.now() / 1000,
      };
    } catch (error) {
      newChatId = await addDuoChat(userId.toString());
    }
    if (!contact && globalUser) {
      setChat({
        id: newChatId.id,
        createdAt: newChatId.createdAt,
        user: globalUser,
        type: ChatType.DUO,
        lastMessage: undefined,
        unreadMessages: 0,
        avatarAvailable: false,
        name: globalUser.name,
      });

      setContact({
        addedAt: undefined,
        alias: globalUser.name,
        user: globalUser,
      });
      setGlobalUser(undefined);
    }
    const fetchedMessages = await getMessagesByChatId(newChatId.id);
    setCurrentMessages(fetchedMessages.messages);
    setTotalMessages(fetchedMessages.totalCount);
    if (webSocket && webSocket.readyState === WebSocket.OPEN) {
      setTimeout(() => {
        checkUnreadMessages(webSocket);
      }, 300);
    }

    dispatch(setCurrentMainBoxState(MainBoxStateEnum.CHAT));
  };

  const handleChatClick = async (currentChat: ChatParams) => {
    if (currentChat.type === ChatType.DUO) {
      setContact({
        addedAt: undefined,
        alias: currentChat.user.name,
        user: currentChat.user,
      });
      setGroupDetails(undefined);
    } else if (currentChat.type === ChatType.GROUP) {
      setGroupDetails(await getGroupChatDetailsById(currentChat.id));
      setContact(undefined);
    }
    setChat(currentChat);
    setCurrentMessages(new Map());
    dispatch(setCurrentMainBoxState(MainBoxStateEnum.CHAT));
    const fetchedMessages = await getMessagesByChatId(currentChat.id);
    setCurrentMessages(fetchedMessages.messages);
    setTotalMessages(fetchedMessages.totalCount);
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
    setIsCreateGroupChatModalOpen(true);
  };

  SIDEBAR_ITEM.buttons.setting.onClick = async () => {
    dispatch(setCurrentInteractiveListState(ListStateEnum.SETTING));
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
    if (chat && offset && offset < totalMessages) {
      const newMessages = await getMessagesByChatId(chat.id, offset);
      setCurrentMessages(prevMessages => {
        const updatedMessages = new Map(prevMessages);
        newMessages.messages.forEach((message, key) => {
          updatedMessages.set(key, message);
        });
        return updatedMessages;
      });
    }
  };
  const handleCreateGroupChatModalClose = () => {
    setIsCreateGroupChatModalOpen(false);
  };

  const handleChangeGroupAvatar = async (file: File, id: string) => {
    await uploadGroupAvatar(file, id);
    const newGroupDetails = {
      ...groupDetails,
      avatarPath: URL.createObjectURL(file),
    } as GroupChatDetails;
    setGroupDetails(newGroupDetails);
  };

  const handleCreateGroupChat = async (
    chatName: string,
    description: string,
    isPrivate: boolean,
    file?: File | null
  ) => {
    const newGroupChat = await createGroupChat(chatName, description, isPrivate);
    let chat: ChatParams = {
      user: {} as UserParams,
      id: newGroupChat.id,
      createdAt: newGroupChat.createdAt,
      type: ChatType.GROUP,
      lastMessage: undefined,
      unreadMessages: 0,
      avatarAvailable: false,
      name: chatName,
    };

    if (file) {
      await handleChangeGroupAvatar(file, newGroupChat.id);
      chat = { ...chat, avatarAvailable: true };
    }
    await fetchChats(chats.size);
    setCurrentSidebarItem(ListStateEnum.CHATS);
    dispatch(setCurrentInteractiveListState(ListStateEnum.CHATS));

    await handleChatClick(chat);
  };

  const handleAddMemberClick = (currentChat: ChatParams, currentContact: ContactParams) => {
    addMemberToGroupChat(webSocket as WebSocket, currentChat.id, currentContact.user.id);
  };

  const handleChangeAvatar = async (file: File) => {
    await uploadAvatar(file);
    const newCurrentUser = {
      ...currentUser,
      avatarPath: URL.createObjectURL(file),
    } as UserParams;
    dispatch(setCurrentUser(newCurrentUser));
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
        <CreateGroupChatModal
          isOpen={isCreateGroupChatModalOpen}
          onClose={handleCreateGroupChatModalClose}
          onSubmit={handleCreateGroupChat}
          confirmButtonTitle=""
        />
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
        onChangeAvatar={handleChangeAvatar}
        contacts={contacts}
        onAddMemberClick={handleAddMemberClick}
        groupDetails={groupDetails}
        chat={chat}
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
