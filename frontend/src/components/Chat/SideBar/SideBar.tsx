import { LockOutlineIcon, SettingOutlineIcon } from "@public/icons";
import React from "react";

import { ICON_SIZE } from "@/components/Chat/SideBar/sideBar.config";
import { CustomButton } from "@/components/CustomButton/CustomButton";

export function SideBar() {
  return (
    <div className="flex flex-col w-1/8 bg-dark-250 p-4 h-screen rounded-l-2xl gap-4 text-blue-500">
      <CustomButton
        variant="square"
        icon={<LockOutlineIcon />}
        iconSize={ICON_SIZE}
      />
      <CustomButton
        variant="square"
        icon={<LockOutlineIcon />}
        iconSize={ICON_SIZE}
      />
      <CustomButton
        variant="square"
        icon={<SettingOutlineIcon />}
        iconSize={ICON_SIZE}
      />
    </div>
  );
}
