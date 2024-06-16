import React, { useRef, useState } from "react";

import { Avatar } from "@/components/Avatar/Avatar";
import { CustomButton } from "@/components/CustomButton/CustomButton";
import { CustomInput } from "@/components/CustomInput/CustomInput";
import { Icon } from "@/components/Icon/Icon";
import { Modal, ModalProps } from "@/components/Modal/Modal";
import { COLORS } from "@/constants/colors";

export type CreateGroupChatModalProps = {
  onSubmit: (chatName: string, description: string, privacy: boolean, file?: File | null) => void;
  onChangeGroupAvatar?: (file: File, id: string) => void;
} & Omit<ModalProps, "onSubmit">;

export function CreateGroupChatModal({
  isOpen,
  onClose: handleClose,
  onSubmit,
  submitButtonColor,
  confirmButtonTitle,
}: Readonly<CreateGroupChatModalProps>) {
  const [chatName, setChatName] = React.useState("");
  const [description, setDescription] = React.useState("");
  const [isPrivacy, setIsPrivacy] = React.useState(false);
  const [preview, setPreview] = useState<string | undefined>(undefined);
  const [file, setFile] = useState<File | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleAvatarChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setPreview(reader.result as string);
      };
      reader.readAsDataURL(file);
      setFile(file);
    }
  };

  const handleAvatarClick = () => {
    fileInputRef.current?.click();
  };

  const handleChatNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setChatName(event.target.value);
  };
  const handleDescriptionChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    setDescription(event.target.value);
  };
  const handlePrivacyChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setIsPrivacy(event.target.checked);
  };

  const handleSubmit = () => {
    onSubmit(chatName, description, isPrivacy, file);
    handleClose();
    setChatName("");
    setDescription("");
    setIsPrivacy(false);
    setPreview(undefined);
    setFile(null);
  };
  return (
    <Modal
      submitButtonColor={submitButtonColor}
      isOpen={isOpen}
      onClose={handleClose}
      onSubmit={() => {}}
      confirmButtonTitle={confirmButtonTitle}
      isButtonDisappear
    >
      <div className="dark:bg-dark-550 px-4 pb-4 pt-5 sm:p-6 sm:pb-4">
        <div className="sm:flex items-center justify-center">
          <div className="mt-3 text-center sm:mt-0 w-full p-4">
            <div className="flex items-center gap-4">
              <CustomButton variant="outline" onClick={handleClose} className="px-1 py-1">
                <Icon name="left-angle" iconSize={28} color={COLORS.blue["200"]} />
              </CustomButton>
              <span className="text-2xl text-blue-200">Create group chat</span>
            </div>
            <div className="flex justify-center items-center pt-4">
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
                <Avatar
                  quality={40}
                  width={100}
                  height={100}
                  item={{ id: "none" }}
                  isGroupAvatar
                  defaultAvatar="/avatars/group.png"
                  preview={preview}
                  alt="Avatar"
                />
              </CustomButton>
            </div>
            <div className="mt-8 mb-8 flex flex-col items-center justify-center gap-10">
              <CustomInput
                value={chatName}
                onChange={handleChatNameChange}
                containerClassName="text-start text-blue-200 w-full text-sm"
                label="Chat name"
                className="text-lg font-normal"
                placeholder="any chat name..."
                innerContainerClassName="rounded-xl"
                icon="pin-outline"
              />
              <label className="text-sm flex flex-col items-start w-full font-thin text-blue-200" htmlFor="description">
                Description{" "}
                <textarea
                  onChange={handleDescriptionChange}
                  value={description}
                  id="description"
                  className="focus:outline-none text-lg resize-none bg-transparent border-2 px-4 py-1 rounded-xl w-full border-gray-500"
                  placeholder="tell about your chat..."
                />
              </label>
            </div>
            <div className="bg-dark-400 w-full flex items-center px-10 py-4 rounded-xl justify-between font-normal">
              <div className="flex gap-4">
                <Icon name="lock-outline" /> Privacy
                {/* eslint-disable-next-line jsx-a11y/label-has-associated-control */}
                <label htmlFor="privacy-switch" className="inline-block ps-[0.15rem] hover:cursor-pointer">
                  <input
                    onChange={handlePrivacyChange}
                    className="me-2 mt-[0.3rem] h-3.5 w-8 appearance-none rounded-[0.4375rem] bg-black/25 before:pointer-events-none before:absolute before:h-3.5 before:w-3.5 before:rounded-full before:bg-transparent before:content-[''] after:absolute after:z-[2] after:-mt-[0.1875rem] after:h-5 after:w-5 after:rounded-full after:border-none after:bg-gray-100 after:shadow-switch-2 after:transition-[background-color_0.2s,transform_0.2s] after:content-[''] checked:bg-primary checked:after:absolute checked:after:z-[2] checked:after:-mt-[3px] checked:after:ms-[1.0625rem] checked:after:h-5 checked:after:w-5 checked:after:rounded-full checked:after:border-none checked:after:bg-primary checked:after:shadow-switch-1 checked:after:transition-[background-color_0.2s,transform_0.2s] checked:after:content-[''] hover:cursor-pointer focus:before:scale-100 focus:before:opacity-[0.12] focus:before:shadow-switch-3 focus:before:shadow-black/60 focus:before:transition-[box-shadow_0.2s,transform_0.2s] focus:after:absolute focus:after:z-[1] focus:after:block focus:after:h-5 focus:after:w-5 focus:after:rounded-full focus:after:content-[''] checked:focus:border-primary checked:focus:bg-primary checked:focus:before:ms-[1.0625rem] checked:focus:before:scale-100 checked:focus:before:shadow-switch-3 checked:focus:before:transition-[box-shadow_0.2s,transform_0.2s] dark:bg-white/25 dark:after:bg-surface-dark dark:checked:bg-blue-200/50 dark:checked:after:bg-blue-500"
                    type="checkbox"
                    role="switch"
                    id="privacy-switch"
                    checked={isPrivacy}
                  />
                </label>
              </div>
              <p className="text-base text-gray-400">Can anyone join?</p>
            </div>
          </div>
        </div>
      </div>
      <div className="dark:bg-dark-550 mb-10 px-4 py-3 sm:flex sm:flex-row sm:px-6 gap-12 items-center justify-center">
        <CustomButton
          onClick={handleSubmit}
          type="button"
          className="font-bold text-lg bg-blue-100 px-12 dark:bg-blue-600"
          variant="flattened"
        >
          Create chat
        </CustomButton>
      </div>
    </Modal>
  );
}
