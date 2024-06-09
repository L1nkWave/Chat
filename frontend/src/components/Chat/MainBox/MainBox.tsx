import React from "react";

import { ContactParams } from "@/api/http/contacts/contacts.types";
import { MainBoxStateEnum } from "@/components/Chat/chat.types";
import { MainBoxProps } from "@/components/Chat/MainBox/mainBox.types";
import { ChatBox } from "@/components/Chat/MainBox/variants/ChatBox/ChatBox";
import { EmptyBox } from "@/components/Chat/MainBox/variants/EmptyBox/EmptyBox";
import { ProfileSettings } from "@/components/Chat/MainBox/variants/ProfileSettings/ProfileSettings";
import { UserInfoBox } from "@/components/Chat/MainBox/variants/UserInfoBox/UserInfoBox";

export function MainBox({
  mainBoxVariant,
  groupDetails,
  contact,
  globalUser,
  messages,
  chat,
  contacts,
  onChangeAvatar,
  onAddContactClick: handleAddContactClick,
  onRemoveContactClick: handleRemoveContactClick,
  onMessageButtonClick: handleMessageButtonClick,
  onSendMessageClick: handleSendMessageClick,
  onHeaderClick: handleHeaderClick,
  onAddMemberClick: handleAddMemberClick,
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
  } else if (chat && mainBoxVariant === MainBoxStateEnum.CHAT) {
    variant = (
      <ChatBox
        contacts={contacts}
        onAddMemberClick={handleAddMemberClick}
        groupDetails={groupDetails}
        chat={chat}
        contact={contact}
        messages={messages}
        onSendMessageClick={handleSendMessageClick}
        onChatHeaderClick={handleHeaderClick}
        loadMessages={loadMessages}
      />
    );
  } else if (mainBoxVariant === MainBoxStateEnum.PROFILE_SETTINGS) {
    variant = <ProfileSettings onChangeAvatar={onChangeAvatar} />;
  }
  return <div className="flex w-full h-screen bg-dark-550 rounded-r-2xl">{variant}</div>;
}
