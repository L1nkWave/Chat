import React from "react";

import { MainBoxProps } from "@/components/Chat/MainBox/mainBox.types";
import { EmptyBox } from "@/components/Chat/MainBox/variants/EmptyBox/EmptyBox";
import { UserInfoBox } from "@/components/Chat/MainBox/variants/UserInfoBox/UserInfoBox";

export function MainBox({ mainBoxVariant, contact }: Readonly<MainBoxProps>) {
  let variant = <EmptyBox />;
  if (mainBoxVariant === "user-info" && contact) {
    variant = <UserInfoBox contact={contact} />;
  } else if (mainBoxVariant === "chat") {
    // variant = <ChatBox chat={chat} />;
  }
  return <div className="flex w-full bg-dark-450 rounded-r-2xl">{variant}</div>;
}
