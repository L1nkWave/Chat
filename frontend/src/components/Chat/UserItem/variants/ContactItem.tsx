import { Avatar } from "@/components/Avatar/Avatar";
import { ContactItemProps } from "@/components/Chat/UserItem/userItem.types";
import { LastSeen } from "@/components/LastSeen/LastSeen";

export function ContactItem({ contact }: Readonly<ContactItemProps>) {
  if (!contact) {
    return null;
  }
  return (
    <div className="flex flex-row">
      <div className="w-14 h-14">
        <Avatar item={contact.user} alt="Contact Avatar" online={contact.user.online} />
      </div>
      <div className="flex flex-col ml-4 justify-center items-start">
        <span className="text-lg">{contact.user.name}</span>
        <LastSeen
          className="text-sm font-medium"
          iconSize={20}
          online={contact.user.online}
          lastSeen={contact.user.lastSeen}
        />
      </div>
    </div>
  );
}
