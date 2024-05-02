import { useRouter } from "next/navigation";
import React, { useContext } from "react";
import { toast } from "react-toastify";

import { logout } from "@/api/http/auth/auth";
import { Avatar } from "@/components/Avatar/Avatar";
import { InteractiveListProps } from "@/components/Chat/InteractiveList/interactiveList.types";
import { ChatList } from "@/components/Chat/InteractiveList/variants/ChatList/ChatList";
import { ContactList } from "@/components/Chat/InteractiveList/variants/ContactList/ContactList";
import { CustomButton } from "@/components/CustomButton/CustomButton";
import { Status } from "@/components/Status/Status";
import { SocketContext } from "@/context/SocketContext/SocketContext";
import { useAppSelector } from "@/lib/hooks";

export function InteractiveList({
  interactiveListVariant,
  interactiveChat,
  interactiveContact,
}: Readonly<InteractiveListProps>) {
  let interactiveList;
  const { webSocket } = useContext(SocketContext);
  const { accessToken } = useAppSelector(state => state.auth);
  const router = useRouter();

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
      console.log("Logged out!");
      router.replace("/sign-in");
    } catch (error) {
      toast.error("Error logging out");
    }
  };

  if (interactiveListVariant === "contacts") {
    interactiveList = (
      <ContactList
        contacts={interactiveContact?.contacts}
        onContactClick={interactiveContact?.onContactClick}
        currentContact={interactiveContact?.currentContact}
      />
    );
  } else if (interactiveListVariant === "chats") {
    interactiveList = <ChatList chats={interactiveChat?.chats} />;
  } else {
    interactiveList = <div>No data</div>;
  }

  return (
    <div className="h-screen flex flex-col min-w-72 w-[55%]">
      <div className="h-screen flex flex-col">{interactiveList}</div>
      <div className="bg-dark-400 px-8 py-4 left-0 bottom-0 w-full flex justify-between items-center h-auto">
        <div className="flex items-center gap-2">
          <Avatar
            item={{
              id: 1,
              avatarPath: undefined,
            }}
            alt="Avatar"
            status={false}
          />
          <div className="flex flex-col gap-0">
            <p className="text-lg">Name</p>
            <div className="flex items-center text-gray-300">
              <Status />
              Active
            </div>
          </div>
        </div>
        <CustomButton
          onClick={handleLogout}
          className="px-1 py-1 text-blue-300 dark:bg-dark-250 w-10 text-2x"
          iconSize={32}
          variant="square"
          icon="sign-out-circle"
        />
      </div>
    </div>
  );
}
