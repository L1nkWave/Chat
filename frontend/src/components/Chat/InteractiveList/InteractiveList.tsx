import { useRouter } from "next/navigation";
import React, { useContext, useEffect } from "react";
import { toast } from "react-toastify";

import { logout } from "@/api/http";
import { Avatar } from "@/components/Avatar/Avatar";
import { ListStateEnum } from "@/components/Chat/chat.types";
import { InteractiveListProps } from "@/components/Chat/InteractiveList/interactiveList.types";
import { ChatList } from "@/components/Chat/InteractiveList/variants/ChatList/ChatList";
import { ContactList } from "@/components/Chat/InteractiveList/variants/ContactList/ContactList";
import { GlobalContactList } from "@/components/Chat/InteractiveList/variants/GlobalContactList/GlobalContactList";
import { Settings } from "@/components/Chat/InteractiveList/variants/Settings/Settings";
import { CustomButton } from "@/components/CustomButton/CustomButton";
import { CustomInput } from "@/components/CustomInput/CustomInput";
import { Status } from "@/components/Status/Status";
import { SocketContext } from "@/context/SocketContext/SocketContext";
import { useAppSelector } from "@/lib/hooks";

export function InteractiveList({
  interactiveListVariant,
  interactiveChat,
  interactiveContact,
  interactiveFindContacts,
  searchValue,
  onChangeSearchValue: handleChangeSearchValue,
}: Readonly<InteractiveListProps>) {
  let interactiveList;
  const { webSocket } = useContext(SocketContext);
  const { accessToken, currentUser } = useAppSelector(state => state.user);
  const router = useRouter();

  useEffect(() => {
    if (!currentUser) {
      router.replace("/sign-in");
    }
  }, [currentUser, router]);

  const handleLogout = async () => {
    if (!webSocket || !accessToken) {
      return;
    }

    try {
      webSocket.onclose = () => {
        console.log("Socket closed");
      };
      webSocket.close();

      await logout();
      localStorage.removeItem("accessToken");
      router.replace("/sign-in");
    } catch (error) {
      toast.error("Error logging out");
    }
  };

  if (!currentUser) {
    return null;
  }

  if (interactiveListVariant === ListStateEnum.CONTACTS) {
    interactiveList = (
      <ContactList
        contacts={interactiveContact?.contacts}
        onContactClick={interactiveContact?.onContactClick}
        currentContact={interactiveContact?.currentContact}
        loadContacts={interactiveContact?.loadContacts}
      />
    );
  } else if (interactiveListVariant === ListStateEnum.CHATS) {
    interactiveList = (
      <ChatList
        chats={interactiveChat?.chats}
        onChatClick={interactiveChat?.onChatClick}
        loadChats={interactiveChat?.loadChats}
      />
    );
  } else if (interactiveListVariant === ListStateEnum.FIND_CONTACTS) {
    interactiveList = (
      <GlobalContactList
        globalContacts={interactiveFindContacts?.globalContacts}
        onGlobalContactClick={interactiveFindContacts?.onGlobalContactClick}
        currentGlobalUser={interactiveFindContacts?.currentGlobalUser}
        loadGlobalContacts={interactiveFindContacts?.loadGlobalContacts}
      />
    );
  } else if (interactiveListVariant === ListStateEnum.SETTING) {
    interactiveList = <Settings />;
  } else {
    interactiveList = <div>No data</div>;
  }

  return (
    <div className="h-screen flex flex-col min-w-72 w-[55%] z-40">
      <div className="h-screen flex flex-col bg-dark-500">
        {(interactiveListVariant === ListStateEnum.FIND_CONTACTS ||
          interactiveListVariant === ListStateEnum.CONTACTS) && (
          <CustomInput
            onChange={e => handleChangeSearchValue(e.target.value)}
            reverseIconPosition
            icon="search-outline"
            iconSize={28}
            placeholder="Type login or name to search a user ..."
            className="p-2.5 dark:bg-gray-550"
            innerContainerClassName="rounded-2xl dark:bg-dark-550 border-none"
            containerClassName="mx-6 mt-4"
            value={searchValue}
          />
        )}
        {interactiveList}
        <div className="bg-dark-400 px-8 py-4 left-0 bottom-0 w-full flex justify-between items-center h-auto">
          <div className="flex items-center gap-2">
            <Avatar width={64} height={64} item={currentUser} alt="Avatar" status={false} />
            <div className="flex flex-col gap-0">
              <p className="text-lg">{currentUser.name}</p>
              <Status online={currentUser.online} textStatus />
            </div>
          </div>
          <CustomButton
            onClick={handleLogout}
            className="px-2.5 py-2.5 dark:text-blue-300 dark:bg-dark-250 text-2x"
            iconSize={32}
            variant="square"
            icon="broken-link-outline"
          />
        </div>
      </div>
    </div>
  );
}
