"use client";

import { useRouter } from "next/navigation";
import React, { useContext, useEffect, useState } from "react";
import { toast } from "react-toastify";

import { getContacts } from "@/api/http/users/users";
import { ContactParams } from "@/api/http/users/users.types";
import { InteractiveList } from "@/components/Chat/InteractiveList/InteractiveList";
import { Contacts } from "@/components/Chat/InteractiveList/interactiveList.types";
import { MainBox } from "@/components/Chat/MainBox/MainBox";
import { SideBar } from "@/components/Chat/SideBar/SideBar";
import { SIDEBAR_ITEM } from "@/components/Chat/SideBar/sidebar.config";
import { SidebarButtonName } from "@/components/Chat/SideBar/sidebar.types";
import { CustomButton } from "@/components/CustomButton/CustomButton";
import { SocketContext } from "@/context/SocketContext/SocketContext";
import { setCurrentInteractiveList, setCurrentMainBox } from "@/lib/features/chat/chatSlice";
import { useAppDispatch, useAppSelector } from "@/lib/hooks";

export function Chat() {
  const dispatch = useAppDispatch();

  const { accessToken } = useAppSelector(state => state.auth);
  const { webSocket, message } = useContext(SocketContext);
  const { currentMainBox, currentInteractiveList } = useAppSelector(state => state.chat);

  const router = useRouter();

  const [contacts, setContacts] = useState<Contacts>([]);
  const [contact, setContact] = useState<ContactParams | undefined>(undefined);
  const [currentSidebarItem, setCurrentSidebarItem] = useState<string>("chat" as SidebarButtonName);

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

  useEffect(() => {
    console.log(message);
  }, [message]);

  const handleContactClick = (currentContact: ContactParams) => {
    setContact(currentContact);
    dispatch(setCurrentMainBox("user-info"));
  };

  SIDEBAR_ITEM.buttons.contact.onClick = () => {
    dispatch(setCurrentInteractiveList("contacts"));
  };

  SIDEBAR_ITEM.buttons.chat.onClick = () => {
    dispatch(setCurrentInteractiveList("chats"));
  };

  SIDEBAR_ITEM.buttons["add-chat"].onClick = () => {
    console.log("add-chat");
  };

  SIDEBAR_ITEM.buttons["find-people"].onClick = () => {
    console.log("find-people");
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
