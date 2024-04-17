import { Avatar } from "@/components/Avatar/Avatar";
import { ContactItemProps } from "@/components/Chat/UserItem/userItem.types";
import { LastSeen } from "@/components/LastSeen/LastSeen";
import { getContactName } from "@/helpers/contactHelpers";

export function ContactItem({ contact, className, onClick, currentContact }: Readonly<ContactItemProps>) {
  if (!contact) {
    return null;
  }
  const handleClick = () => {
    if (onClick) {
      onClick(contact);
    }
  };

  const isActive = currentContact?.user.id === contact.user.id;

  return (
    <button
      onClick={handleClick}
      type="button"
      className={`${isActive ? "bg-dark-200" : "bg-dark-300"} hover:bg-dark-200 flex flex-row outline-none p-4 rounded-lg ${className}`}
    >
      <div className="w-14 h-14">
        <Avatar item={contact.user} alt="Contact Avatar" online={contact.user.online} />
      </div>
      <div className="flex flex-col ml-4 justify-center items-start">
        <span className="text-lg">{getContactName(contact)}</span>
        <LastSeen
          className="text-sm font-medium"
          iconSize={20}
          online={contact.user.online}
          lastSeen={contact.user.lastSeen}
        />
      </div>
    </button>
  );
}
