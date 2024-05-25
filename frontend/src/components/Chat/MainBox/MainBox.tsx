import React from "react";

import { ContactParams } from "@/api/http/contacts/contacts.types";
import { MainBoxStateEnum } from "@/components/Chat/chat.types";
import { MainBoxProps } from "@/components/Chat/MainBox/mainBox.types";
import { ChatBox } from "@/components/Chat/MainBox/variants/ChatBox/ChatBox";
import { EmptyBox } from "@/components/Chat/MainBox/variants/EmptyBox/EmptyBox";
import { UserInfoBox } from "@/components/Chat/MainBox/variants/UserInfoBox/UserInfoBox";

export function MainBox({
  mainBoxVariant,
  contact,
  globalUser,
  messages,
  chatId,
  onAddContactClick: handleAddContactClick,
  onRemoveContactClick: handleRemoveContactClick,
  onMessageButtonClick: handleMessageButtonClick,
  onSendMessageClick: handleSendMessageClick,
  onHeaderClick: handleHeaderClick,
  loadMessages,
}: Readonly<MainBoxProps>) {
  let variant = <EmptyBox />;
  if (mainBoxVariant === MainBoxStateEnum.USER_INFO && contact) {
    variant = (
      <UserInfoBox
        contact={contact}
        onAddContactClick={handleAddContactClick}
        onRemoveContactClick={handleRemoveContactClick}
        onMessageButtonClick={handleMessageButtonClick}
      />
    );
  }
  if (mainBoxVariant === MainBoxStateEnum.USER_INFO && globalUser) {
    const globalContact: ContactParams = {
      alias: undefined,
      addedAt: undefined,
      user: globalUser,
    };
    variant = (
      <UserInfoBox
        contact={globalContact}
        onAddContactClick={handleAddContactClick}
        onRemoveContactClick={handleRemoveContactClick}
        onMessageButtonClick={handleMessageButtonClick}
      />
    );
  } else if (contact && chatId && mainBoxVariant === MainBoxStateEnum.CHAT) {
    variant = (
      <ChatBox
        contact={contact}
        messages={messages}
        onSendMessageClick={handleSendMessageClick}
        onChatHeaderClick={handleHeaderClick}
        loadMessages={loadMessages}
      />
    );
  }
  return <div className="flex w-full h-screen bg-dark-550 rounded-r-2xl">{variant}</div>;
}
