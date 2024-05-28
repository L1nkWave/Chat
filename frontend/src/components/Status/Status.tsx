import React from "react";

import { StatusProps } from "@/components/Status/status.types";
import { UserStatus } from "@/lib/features/user/userSlice.types";

export function Status({ className, textStatus, online, classNameTextContainer, ...props }: Readonly<StatusProps>) {
  if (textStatus) {
    return (
      <div className={`flex items-center text-gray-300 ${!!classNameTextContainer && classNameTextContainer}`}>
        <Status online />
        {online ? UserStatus.ONLINE : UserStatus.OFFLINE}
      </div>
    );
  }

  return online && <div className={`bg-green rounded-full w-2.5 h-2.5 mr-1 ${!!className && className}`} {...props} />;
}
