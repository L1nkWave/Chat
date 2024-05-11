import { Avatar } from "@/components/Avatar/Avatar";
import { InfoTextBox } from "@/components/Chat/MainBox/variants/UserInfoBox/InfoBox/InfoTextBox";
import { InfoIconShape } from "@/components/Chat/MainBox/variants/UserInfoBox/InfoIconShape/InfoIconShape";
import { UserInfoBoxProps } from "@/components/Chat/MainBox/variants/UserInfoBox/userInfoBox.types";
import { CustomButton } from "@/components/CustomButton/CustomButton";
import { Icon } from "@/components/Icon/Icon";
import { LastSeen } from "@/components/LastSeen/LastSeen";
import { Status } from "@/components/Status/Status";
import { getContactName } from "@/helpers/contactHelpers";

export function UserInfoBox({
  contact,
  onRemoveContactClick,
  onAddContactClick,
  onMessageButtonClick,
}: Readonly<UserInfoBoxProps>) {
  const isContactAdded = !!contact.addedAt;
  const handleRemoveContactClick = () => {
    if (contact && onRemoveContactClick) {
      onRemoveContactClick(contact.user.id.toString());
    }
  };
  const handleAddContactClick = () => {
    if (contact && onAddContactClick) {
      onAddContactClick(contact.user.id.toString(), contact.user.name);
    }
  };
  const handleMessageButtonClick = () => {
    if (contact && onMessageButtonClick) {
      onMessageButtonClick(contact.user.id.toString());
    }
  };
  return (
    <div className="m-6 flex flex-col w-full h-fit border-2 rounded-2xl border-dark-100 bg-dark-350">
      <div className="p-16">
        <div className="flex">
          <Avatar className="w-32 h-32" item={contact.user} alt="Avatar" />
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
            onClick={isContactAdded ? handleRemoveContactClick : handleAddContactClick}
          >
            <Icon name={isContactAdded ? "remove-circle-outline" : "add-circle-outline"} iconSize={32} />
          </CustomButton>
        </div>
      </div>
    </div>
  );
}
