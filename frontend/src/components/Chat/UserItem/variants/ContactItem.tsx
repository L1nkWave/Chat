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
      className={`${isActive ? "bg-dark-200" : "bg-dark-300"} hover:bg-dark-200 flex flex-row items-center outline-none p-4 rounded-lg ${className ?? ""}`}
    >
      <div className="min-w-max min-h-max">
        <Avatar item={contact.user} alt="Contact Avatar" online={contact.user.online} width={64} height={64} />
      </div>
      <div className="flex flex-col ml-4 justify-center items-start">
        <span className="text-lg">{getContactName(contact)}</span>
        <LastSeen
          className="text-sm font-medium"
          iconSize={20}
          lastSeen={contact.user.lastSeen}
          online={contact.user.online}
        />
      </div>
    </button>
  );
}
