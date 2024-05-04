import React from "react";

import { ContactParams } from "@/api/http/contacts/contacts.types";
import { MainBoxProps } from "@/components/Chat/MainBox/mainBox.types";
import { EmptyBox } from "@/components/Chat/MainBox/variants/EmptyBox/EmptyBox";
import { UserInfoBox } from "@/components/Chat/MainBox/variants/UserInfoBox/UserInfoBox";

export function MainBox({
  mainBoxVariant,
  contact,
  globalUser,
  onAddContactClick: handleAddContactClick,
  onRemoveContactClick: handleRemoveContactClick,
}: Readonly<MainBoxProps>) {
  let variant = <EmptyBox />;
  if (mainBoxVariant === "user-info" && contact) {
    variant = (
      <UserInfoBox
        contact={contact}
        onAddContactClick={handleAddContactClick}
        onRemoveContactClick={handleRemoveContactClick}
      />
    );
  }
  if (mainBoxVariant === "user-info" && globalUser) {
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
      />
    );
  } else if (mainBoxVariant === "chat") {
    variant = <div>Chat</div>;
  }
  return <div className="flex w-full bg-dark-450 rounded-r-2xl">{variant}</div>;
}
