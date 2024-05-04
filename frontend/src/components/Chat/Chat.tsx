"use client";

import React, { useContext, useState } from "react";
import { toast } from "react-toastify";

import { addDuoChat, getChats } from "@/api/http/chat/chat";
import { addContact, getContacts, removeContact, searchContacts } from "@/api/http/contacts/contacts";
import { ChatParams, ContactParams, UserParams } from "@/api/http/contacts/contacts.types";
import { ListStateEnum } from "@/components/Chat/chat.types";
import { InteractiveList } from "@/components/Chat/InteractiveList/InteractiveList";
import { Contacts } from "@/components/Chat/InteractiveList/interactiveList.types";
import { MainBox } from "@/components/Chat/MainBox/MainBox";
import { SideBar } from "@/components/Chat/SideBar/SideBar";
import { SIDEBAR_ITEM } from "@/components/Chat/SideBar/sidebar.config";
import { SidebarButtonName } from "@/components/Chat/SideBar/sidebar.types";
import { CustomButton } from "@/components/CustomButton/CustomButton";
import { SocketContext } from "@/context/SocketContext/SocketContext";
import { useAccessTokenEffect } from "@/hooks/useAccessTokenEffect";
import { setCurrentInteractiveListState, setCurrentMainBoxState } from "@/lib/features/chat/chatSlice";
import { useAppDispatch, useAppSelector } from "@/lib/hooks";

export function Chat() {
  const dispatch = useAppDispatch();

  const { currentMainBoxState, currentInteractiveListState } = useAppSelector(state => state.chat);

  const { message } = useContext(SocketContext);

  const [contacts, setContacts] = useState<Contacts>([]);
  const [contact, setContact] = useState<ContactParams | undefined>(undefined);

  const [globalUsers, setGlobalUsers] = useState<UserParams[]>([]);
  const [globalUser, setGlobalUser] = useState<UserParams | undefined>(undefined);

  const [chats, setChats] = useState<ChatParams[]>([]);

  const [currentSidebarItem, setCurrentSidebarItem] = useState<string>("chat" as SidebarButtonName);

  useAccessTokenEffect(() => {
    const fetchContacts = async () => {
      try {
        const fetchedContacts = await getContacts();
        setContacts(fetchedContacts);
      } catch (error) {
        toast.error("Error fetching contacts");
      }
    };
    fetchContacts();
  }, [contacts]);

  useAccessTokenEffect(() => {
    const fetchGlobalContacts = async () => {
      try {
        const fetchedContacts = await searchContacts();
        setGlobalUsers(fetchedContacts);
      } catch (error) {
        toast.error("Error fetching global users");
      }
    };
    fetchGlobalContacts();
  }, [globalUsers]);

  useAccessTokenEffect(() => {
    const fetchChats = async () => {
      try {
        const fetchedChats = await getChats();
        setChats(fetchedChats);
        console.log("Chats:", fetchedChats);
      } catch (error) {
        toast.error("Error fetching chats");
      }
    };
    fetchChats();
  }, [chats]);

  useAccessTokenEffect(() => {
    console.log(message);
  }, [message]);

  const handleContactClick = (currentContact: ContactParams) => {
    setGlobalUser(undefined);
    setContact(currentContact);
    dispatch(setCurrentMainBoxState("user-info"));
  };

  const handleGlobalContactClick = (currentGlobalUser: UserParams) => {
    setContact(undefined);
    setGlobalUser(currentGlobalUser);
    dispatch(setCurrentMainBoxState("user-info"));
  };

  const handleAddContact = async (userId: string, alias: string) => {
    try {
      await addContact(userId, alias);
      await addDuoChat(userId);
      setGlobalUsers(prevGlobalUsers =>
        prevGlobalUsers.filter(prevGlobalUser => prevGlobalUser.id.toString() !== userId)
      );
    } catch (error) {
      toast.error("Error adding contact");
    }
  };
  const handleRemoveContact = async (userId: string) => {
    try {
      await removeContact(userId);
      setContacts(prevContacts => prevContacts.filter(prevContact => prevContact.user.id.toString() !== userId));
    } catch (error) {
      toast.error("Error adding contact");
    }
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
      />
    </div>
  );
}
