import { useState } from "react";

import { Avatar } from "@/components/Avatar/Avatar";
import { InfoTextBox } from "@/components/Chat/MainBox/variants/UserInfoBox/InfoBox/InfoTextBox";
import { InfoIconShape } from "@/components/Chat/MainBox/variants/UserInfoBox/InfoIconShape/InfoIconShape";
import { UserInfoBoxProps } from "@/components/Chat/MainBox/variants/UserInfoBox/userInfoBox.types";
import { CustomButton } from "@/components/CustomButton/CustomButton";
import { CustomInput } from "@/components/CustomInput/CustomInput";
import { Icon } from "@/components/Icon/Icon";
import { LastSeen } from "@/components/LastSeen/LastSeen";
import { Modal } from "@/components/Modal/Modal";
import { Status } from "@/components/Status/Status";
import { COLORS } from "@/constants/colors";
import { getContactName } from "@/helpers/contactHelpers";
import { useAppSelector } from "@/lib/hooks";

export function UserInfoBox({
  contact,
  onRemoveContactClick,
  onAddContactClick,
  onMessageButtonClick,
}: Readonly<UserInfoBoxProps>) {
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [isRemoveModalOpen, setIsRemoveModalOpen] = useState(false);
  const [alias, setAlias] = useState("");
  const isContactAdded = !!contact.addedAt;
  const { currentUser } = useAppSelector(state => state.user);
  if (!currentUser) {
    return null;
  }
  const handleAddContactOpenModal = () => {
    setIsAddModalOpen(true);
  };
  const handleAddContactModalClose = () => {
    setIsAddModalOpen(false);
  };
  const handleRemoveContactOpenModal = () => {
    setIsRemoveModalOpen(true);
  };
  const handleRemoveContactModalClose = () => {
    setIsRemoveModalOpen(false);
  };
  const handleAddContactClick = () => {
    if (contact && onAddContactClick) {
      onAddContactClick(contact.user.id.toString(), alias || contact.user.name);
      setIsAddModalOpen(false);
      setAlias("");
    }
  };
  const handleRemoveContactClick = () => {
    if (contact && onRemoveContactClick) {
      onRemoveContactClick(contact.user.id.toString());
      setIsRemoveModalOpen(false);
    }
  };
  const handleMessageButtonClick = () => {
    if (contact && onMessageButtonClick) {
      onMessageButtonClick(contact.user.id.toString());
    }
  };
  return (
    <div className="m-6 flex flex-col w-full h-fit border-2 rounded-2xl border-dark-100 bg-dark-350">
      <Modal
        onSubmit={handleAddContactClick}
        isOpen={isAddModalOpen}
        onClose={handleAddContactModalClose}
        confirmButtonTitle="Add contact"
      >
        <div className="dark:bg-dark-550 px-4 pb-4 pt-5 sm:p-6 sm:pb-4">
          <div className="sm:flex items-center justify-center">
            <div className="mt-3 text-center sm:mt-0">
              <div className="flex justify-center gap-4 p-6">
                <Avatar item={currentUser} alt="Avatar" className="w-20 h-20" width={64} height={64} />
                <Icon name="link-outline" iconSize={32} color={COLORS.blue["500"]} />
                <Avatar item={contact.user} alt="Avatar" className="w-20 h-20" width={64} height={64} />
              </div>
              <h1 className="text-2xl text-blue-200">
                Add <span className="font-bold text-blue-100">{contact.user.name}</span> to contacts?
              </h1>
              <p className="text-blue-300 font-thin">You will can easily access this person in the future.</p>
              <div className="mt-8 mb-8 flex flex-col items-center justify-center">
                <CustomInput
                  value={alias}
                  onChange={e => setAlias(e.target.value)}
                  containerClassName="text-start text-blue-200 w-full"
                  label="Alias"
                  innerContainerClassName="rounded-xl"
                  placeholder={contact.user.name}
                  icon="pin-outline"
                />
                <p className="text-blue-400 text-sm">
                  You can give an alias to your new contact (not visible to others)
                </p>
              </div>
            </div>
          </div>
        </div>
      </Modal>
      <Modal
        onSubmit={handleRemoveContactClick}
        isOpen={isRemoveModalOpen}
        onClose={handleRemoveContactModalClose}
        confirmButtonTitle="Remove contact"
        submitButtonColor="dark:bg-red-200"
      >
        <div className="dark:bg-dark-550 px-4 pb-4 pt-5 sm:p-6 sm:pb-4">
          <div className="sm:flex items-center justify-center">
            <div className="mt-3 text-center sm:mt-0">
              <div className="flex justify-center gap-4 p-6">
                <Avatar item={currentUser} alt="Avatar" className="w-20 h-20" width={64} height={64} />
                <Icon name="link-outline" iconSize={32} color={COLORS.red["200"]} />
                <Avatar item={contact.user} alt="Avatar" width={64} height={64} className="w-20 h-20" />
              </div>
              <h1 className="text-2xl text-blue-400">
                Add <span className="font-bold text-red-200">{contact.user.name}</span> to contacts?
              </h1>
              <p className="text-blue-300 font-thin">
                Removes Jamel Eusebio from your contacts list. Chat history still exists.
              </p>
            </div>
          </div>
        </div>
      </Modal>

      <div className="p-16">
        <div className="flex">
          <Avatar className="w-32 h-32" item={contact.user} alt="Avatar" width={128} height={128} />
          <div className="px-10 flex flex-col justify-center">
            <h1 className="text-3xl text-gray-200">{getContactName(contact)}</h1>
            {contact.user.online ? (
              <Status online={contact.user.online} textStatus classNameTextContainer="font-semibold" />
            ) : (
              <LastSeen
                lastSeen={contact.user.lastSeen}
                iconSize={24}
                className="font-semibold"
                textColor="text-blue-300"
                online={contact.user.online}
              />
            )}
          </div>
        </div>

        <div className="py-8">
          <InfoTextBox>
            <InfoIconShape icon="link-outline" />
            {contact.user.username}
          </InfoTextBox>
          <InfoTextBox>
            <InfoIconShape icon="list-outline" />
            {contact.user.bio}
          </InfoTextBox>
        </div>

        <div className="flex justify-center gap-10 pt-10">
          <CustomButton variant="outline" type="button" className="text-blue-200">
            <Icon name="left-angle" iconSize={32} />
          </CustomButton>
          <CustomButton
            variant="outline"
            type="button"
            className="text-blue-200 text-2xl px-10"
            onClick={handleMessageButtonClick}
          >
            <Icon name="pen-with-message" iconSize={32} />
            Message
          </CustomButton>
          <CustomButton
            variant="outline"
            type="button"
            className="text-blue-200"
            onClick={isContactAdded ? handleRemoveContactOpenModal : handleAddContactOpenModal}
          >
            <Icon name={isContactAdded ? "remove-circle-outline" : "add-circle-outline"} iconSize={32} />
          </CustomButton>
        </div>
      </div>
    </div>
  );
}
