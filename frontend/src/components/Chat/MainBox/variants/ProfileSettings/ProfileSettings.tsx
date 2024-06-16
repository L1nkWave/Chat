import React, { useRef, useState } from "react";

import { Avatar } from "@/components/Avatar/Avatar";
import { ProfileSettingsProps } from "@/components/Chat/MainBox/variants/ProfileSettings/profileSettingsProps.types";
import { InfoTextBox } from "@/components/Chat/MainBox/variants/UserInfoBox/InfoBox/InfoTextBox";
import { InfoIconShape } from "@/components/Chat/MainBox/variants/UserInfoBox/InfoIconShape/InfoIconShape";
import { CustomButton } from "@/components/CustomButton/CustomButton";
import { useAppSelector } from "@/lib/hooks";

export function ProfileSettings({ onChangeAvatar }: ProfileSettingsProps) {
  const { currentUser } = useAppSelector(state => state.user);
  const [preview, setPreview] = useState<string | undefined>(undefined);
  const fileInputRef = useRef<HTMLInputElement>(null);

  if (!currentUser) {
    return null;
  }

  const handleAvatarChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setPreview(reader.result as string);
      };
      reader.readAsDataURL(file);
      if (onChangeAvatar) {
        onChangeAvatar(file);
      }
    }
  };

  const handleAvatarClick = () => {
    fileInputRef.current?.click();
  };

  return (
    <div className="w-full flex justify-center items-start">
      <div className="flex flex-col justify-center items-center rounded-2xl p-10 pt-32 w-full z-10">
        <input
          accept="image/png, image/gif, image/jpeg"
          type="file"
          ref={fileInputRef}
          className="hidden"
          onChange={handleAvatarChange}
        />
        <CustomButton
          variant="transparent"
          className="cursor-pointer max-w-[140px] max-h-[140px]"
          onClick={handleAvatarClick}
        >
          <Avatar quality={40} width={140} height={140} item={currentUser} preview={preview} alt="Avatar" />
        </CustomButton>
        <h1 className="text-4xl text-blue-100">{currentUser.name}</h1>
        <h2 className="text-2xl text-gray-300">{currentUser.username}</h2>
        <InfoTextBox>
          <InfoIconShape icon="list-outline" />
          {currentUser.bio}
        </InfoTextBox>
      </div>
    </div>
  );
}
