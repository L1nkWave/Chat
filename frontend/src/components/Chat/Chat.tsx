"use client";

import { useRouter } from "next/navigation";
import React, { useContext, useEffect, useState } from "react";
import { toast } from "react-toastify";

import { getContacts } from "@/api/http/users/users";
import { ContactParams } from "@/api/http/users/users.types";
import { SIDEBAR_ICON_SIZE } from "@/components/Chat/chat.config";
import { InteractiveList } from "@/components/Chat/InteractiveList/InteractiveList";
import { Contacts } from "@/components/Chat/InteractiveList/interactiveList.types";
import { MainBox } from "@/components/Chat/MainBox/MainBox";
import { SideBar } from "@/components/Chat/SideBar/SideBar";
import { CustomButton } from "@/components/CustomButton/CustomButton";
import { SocketContext } from "@/context/SocketContext/SocketContext";
import { setCurrentInteractiveList, setCurrentMainBox } from "@/lib/features/chat/chatSlice";
import { useAppDispatch, useAppSelector } from "@/lib/hooks";

export function Chat() {
  const dispatch = useAppDispatch();

  const { accessToken } = useAppSelector(state => state.auth);
  const { webSocket } = useContext(SocketContext);
  const { currentMainBox, currentInteractiveList } = useAppSelector(state => state.chat);

  const router = useRouter();

  const [contacts, setContacts] = useState<Contacts>([]);
  const [contact, setContact] = useState<ContactParams | undefined>(undefined);

  useEffect(() => {
    if (!accessToken) {
      router.push("/sign-in");
      return;
    }
    const fetchContacts = async () => {
      try {
        const fetchedContacts = await getContacts();
        setContacts(fetchedContacts);
      } catch (error) {
        toast.error("Error fetching contacts");
      }
    };
    fetchContacts();
  }, [accessToken, router, webSocket]);

  const handleContactsClick = () => {
    dispatch(setCurrentInteractiveList("contacts"));
  };

  const handleChatsClick = async () => {
    dispatch(setCurrentInteractiveList("chats"));
  };

  const handleContactClick = (currentContact: ContactParams) => {
    setContact(currentContact);
    dispatch(setCurrentMainBox("user-info"));
  };

  return (
    <div className="w-screen flex px-64">
      <SideBar>
        <CustomButton
          variant="square"
          icon="chat-plus-outline"
          iconSize={SIDEBAR_ICON_SIZE}
          onClick={handleChatsClick}
        />
        <CustomButton
          variant="square"
          icon="group-outline"
          iconSize={SIDEBAR_ICON_SIZE}
          onClick={handleContactsClick}
        />
        <CustomButton variant="square" iconSize={SIDEBAR_ICON_SIZE} icon="setting-outline" />
      </SideBar>
      <InteractiveList
        interactiveListVariant={currentInteractiveList}
        interactiveContact={{
          currentContact: contact,
          contacts,
          onContactClick: handleContactClick,
        }}
        interactiveChat={{
          chats: [],
        }}
      />
      <MainBox mainBoxVariant={currentMainBox} contact={contact} />
    </div>
  );
}
