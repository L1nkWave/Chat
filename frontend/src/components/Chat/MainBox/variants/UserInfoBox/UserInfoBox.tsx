import { Avatar } from "@/components/Avatar/Avatar";
import { InfoTextBox } from "@/components/Chat/MainBox/variants/UserInfoBox/InfoBox/InfoTextBox";
import { InfoIconShape } from "@/components/Chat/MainBox/variants/UserInfoBox/InfoIconShape/InfoIconShape";
import { UserInfoBoxProps } from "@/components/Chat/MainBox/variants/UserInfoBox/userInfoBox.types";
import { CustomButton } from "@/components/CustomButton/CustomButton";
import { Icon } from "@/components/Icon/Icon";
import { LastSeen } from "@/components/LastSeen/LastSeen";
import { getContactName } from "@/helpers/contactHelpers";

export function UserInfoBox({ contact, onRemoveContactClick, onAddContactClick }: Readonly<UserInfoBoxProps>) {
  const isContactAdded = !!contact.addedAt;
  const handleRemoveContactClick = () => {
    if (contact.user.id && onRemoveContactClick) {
      onRemoveContactClick(contact.user.id.toString());
    }
  };
  const handleAddContactClick = () => {
    if (contact.user.id && onAddContactClick) {
      onAddContactClick(contact.user.id.toString(), contact.user.name);
    }
  };
  return (
    <div className="m-6 flex flex-col w-full h-fit border-2 rounded-2xl border-dark-100 bg-dark-350">
      <div className="p-16">
        <div className="flex">
          <Avatar className="w-32 h-32" item={contact.user} alt="Avatar" />
          <div className="px-10 flex flex-col justify-center">
            <h1 className="text-3xl text-gray-200">{getContactName(contact)}</h1>
            <LastSeen
              online={contact.user.online}
              lastSeen={contact.user.lastSeen}
              iconSize={24}
              className="font-semibold"
              textColor="text-blue-300"
            />
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
          <CustomButton variant="outline" type="button" className="text-blue-200 text-2xl px-10">
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
