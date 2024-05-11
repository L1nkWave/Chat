"use client";

import React, { useCallback, useContext, useState } from "react";
import { toast } from "react-toastify";

import { addDuoChat, getChatByUserId, getChats } from "@/api/http/chat/chat";
import { addContact, getContacts, removeContact } from "@/api/http/contacts/contacts";
import { ContactParams, UserParams } from "@/api/http/contacts/contacts.types";
import { searchUser } from "@/api/http/user/user";
import { ListStateEnum, MainBoxStateEnum } from "@/components/Chat/chat.types";
import { InteractiveList } from "@/components/Chat/InteractiveList/InteractiveList";
import { ChatMap, ContactsMap, UserMap } from "@/components/Chat/InteractiveList/interactiveList.types";
import { MainBox } from "@/components/Chat/MainBox/MainBox";
import { SideBar } from "@/components/Chat/SideBar/SideBar";
import { SIDEBAR_ITEM } from "@/components/Chat/SideBar/sidebar.config";
import { SidebarButtonName } from "@/components/Chat/SideBar/sidebar.types";
import { CustomButton } from "@/components/CustomButton/CustomButton";
import { SocketContext } from "@/context/SocketContext/SocketContext";
import { MessageAction } from "@/context/SocketContext/socketContext.types";
import { lastSeenDateNow } from "@/helpers/contactHelpers";
import { useAccessTokenEffect } from "@/hooks/useAccessTokenEffect";
import { setCurrentInteractiveListState, setCurrentMainBoxState } from "@/lib/features/chat/chatSlice";
import { useAppDispatch, useAppSelector } from "@/lib/hooks";

export function Chat() {
  const dispatch = useAppDispatch();

  const { currentMainBoxState, currentInteractiveListState } = useAppSelector(state => state.chat);

  const { message } = useContext(SocketContext);

  const [contacts, setContacts] = useState<ContactsMap>(new Map());
  const [contact, setContact] = useState<ContactParams | undefined>(undefined);

  const [globalUsers, setGlobalUsers] = useState<UserMap>(new Map());
  const [globalUser, setGlobalUser] = useState<UserParams | undefined>(undefined);

  const [chats, setChats] = useState<ChatMap>(new Map());

  const [currentSidebarItem, setCurrentSidebarItem] = useState<string>("chat" as SidebarButtonName);

  const fetchGlobalContacts = useCallback(async () => {
    try {
      const fetchedContacts = await searchUser();
      setGlobalUsers(fetchedContacts);
    } catch (error) {
      toast.error("Error fetching global users");
    }
  }, []);

  const fetchContacts = useCallback(async () => {
    try {
      const fetchedContacts = await getContacts();
      setContacts(fetchedContacts);
    } catch (error) {
      toast.error("Error fetching contacts");
    }
  }, []);

  useAccessTokenEffect(() => {
    fetchContacts();
  }, []);

  useAccessTokenEffect(() => {
    fetchGlobalContacts();
  }, []);

  useAccessTokenEffect(() => {
    const fetchChats = async () => {
      try {
        const fetchedChats = await getChats();
        setChats(fetchedChats);
      } catch (error) {
        toast.error("Error fetching chats");
      }
    };
    fetchChats();
  }, []);

  useAccessTokenEffect(() => {
    console.log("message", message);
    if (message?.action === MessageAction.OFFLINE) {
      setContacts(prevContacts => {
        const updatedContact = prevContacts.get(message.senderId);
        if (updatedContact) {
          updatedContact.user.online = false;
          updatedContact.user.lastSeen = lastSeenDateNow();
          return new Map(prevContacts).set(message.senderId, updatedContact);
        }
        return prevContacts;
      });
    } else if (message?.action === MessageAction.ONLINE) {
      setContacts(prevContacts => {
        const updatedContact = prevContacts.get(message.senderId);
        if (updatedContact) {
          updatedContact.user.online = true;
          return new Map(prevContacts).set(message.senderId, updatedContact);
        }
        return prevContacts;
      });
    }
  }, [message]);

  const handleContactClick = (currentContact: ContactParams) => {
    setGlobalUser(undefined);
    setContact(currentContact);
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
        updatedUsers.delete(parseInt(userId, 10));
        return updatedUsers;
      });
      await fetchContacts();

      if (globalUser) {
        setContact(contacts.get(globalUser.id));
      }
      setGlobalUser(undefined);
    } catch (error) {
      toast.error("Error adding contact");
    }
  };

  const handleRemoveContact = async (userId: string) => {
    try {
      await removeContact(userId);

      setContacts(prevContacts => {
        const updatedContacts = new Map(prevContacts);
        updatedContacts.delete(parseInt(userId, 10));
        return updatedContacts;
      });

      if (contact) {
        setGlobalUser(contact.user);
      }
      setContact(undefined);

      await fetchGlobalContacts();
    } catch (error) {
      toast.error("Error adding contact");
    }
  };

  const handleMessageButtonClick = async (userId: string) => {
    const chatId = await getChatByUserId(userId);
    console.log(chatId);
    dispatch(setCurrentMainBoxState(MainBoxStateEnum.CHAT));
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
        }}
        interactiveChat={{
          chats,
        }}
        interactiveFindContacts={{
          currentGlobalUser: globalUser,
          globalContacts: globalUsers,
          onGlobalContactClick: handleGlobalContactClick,
        }}
      />
      <MainBox
        mainBoxVariant={currentMainBoxState}
        contact={contact}
        globalUser={globalUser}
        onAddContactClick={handleAddContact}
        onRemoveContactClick={handleRemoveContact}
        onMessageButtonClick={handleMessageButtonClick}
      />
    </div>
  );
}
