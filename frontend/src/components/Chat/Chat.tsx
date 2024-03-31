"use client";

import Image from "next/image";
import { useRouter } from "next/navigation";
import React, { useEffect, useState } from "react";
import { toast } from "react-toastify";

import { getContacts } from "@/api/http/users/users";
import { ContactParams } from "@/api/http/users/users.types";
import { SIDEBAR_ICON_SIZE } from "@/components/Chat/chat.config";
import { InteractiveList } from "@/components/Chat/InteractiveList/InteractiveList";
import { MainBox } from "@/components/Chat/MainBox/MainBox";
import { SideBar } from "@/components/Chat/SideBar/SideBar";
import { CustomButton } from "@/components/CustomButton/CustomButton";
import { decodeToken } from "@/helpers/DecodeToken/decodeToken";
import { setCurrentInteractiveList } from "@/lib/features/chat/chatSlice";
import { useAppDispatch, useAppSelector } from "@/lib/hooks";

export function Chat() {
  const dispatch = useAppDispatch();

  const { accessToken } = useAppSelector(state => state.auth);
  const { webSocket } = useAppSelector(state => state.socket);
  const { currentMainBox, currentInteractiveList } = useAppSelector(state => state.chat);

  const router = useRouter();

  const [contacts, setContacts] = useState<ContactParams[]>([]);

  useEffect(() => {
    const fetchContacts = async () => {
      try {
        let fetchedContacts = await getContacts();
        fetchedContacts = fetchedContacts.map(contact => {
          if (contact.user.id === 2)
            return {
              ...contact,
              user: {
                ...contact.user,
                online: true,
              },
            };
          return contact;
        });

        setContacts(fetchedContacts);
      } catch (error) {
        toast.error("Error fetching contacts");
      }
    };

    fetchContacts();
    if (!accessToken) {
      return router.push("/sign-in");
    }
    if (!webSocket) {
      return () => {};
    }

    const payload = decodeToken(accessToken);
    if (!payload) {
      return router.push("/sign-in");
    }

    webSocket.onopen = event => {
      console.log("WebSocket connection opened:", event);
    };

    webSocket.onmessage = event => {
      console.log("WebSocket connection message:", event);
    };

    return () => {
      if (!webSocket) return;
      webSocket.removeEventListener("open", () => {});
      webSocket.removeEventListener("message", () => {});
      webSocket.removeEventListener("close", () => {});
    };
  }, [accessToken, router, webSocket]);

  const handleContactsClick = () => {
    dispatch(setCurrentInteractiveList("contacts"));
  };

  const handleChatsClick = () => {
    dispatch(setCurrentInteractiveList("chats"));
  };

  if (webSocket?.readyState === WebSocket.CLOSED) {
    return (
      <div className="w-full h-full">
        <Image
          width={400}
          height={500}
          className="w-full rounded-lg"
          src="/images/ChatPage/backend-fall.gif"
          alt="Backend is currently unstable, please wait."
        />
      </div>
    );
  }

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
      <InteractiveList interactiveListVariant={currentInteractiveList} contacts={contacts} chats={[]} />
      <MainBox mainBoxVariant={currentMainBox} />
    </div>
  );
}
