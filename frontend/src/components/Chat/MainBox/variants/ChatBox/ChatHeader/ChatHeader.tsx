import { Avatar } from "@/components/Avatar/Avatar";
import { ChatHeaderProps } from "@/components/Chat/MainBox/variants/ChatBox/ChatHeader/chatHeader.types";
import { LastSeen } from "@/components/LastSeen/LastSeen";
import { Status } from "@/components/Status/Status";

export function ChatHeader({ contact, onChatHeaderClick }: Readonly<ChatHeaderProps>) {
  const handleClick = () => {
    if (onChatHeaderClick) {
      onChatHeaderClick(contact);
    }
  };
  return (
    <div className="bg-dark-600 border-2 border-dark-150 rounded-2xl p-4 py-4 m-6 mb-0">
      <button type="button" className="flex rounded-2xl items-center" onClick={handleClick}>
        <Avatar className="mx-5" item={contact.user} alt="Avatar" />
        <div className="flex flex-col justify-center">
          <h1 className="text-xl text-gray-100 text-start">{contact.alias || contact.user.name}</h1>
          <div className="flex items-center">
            {contact.user.online ? (
              <Status online={contact.user.online} textStatus />
            ) : (
              <LastSeen lastSeen={contact.user.lastSeen} online={contact.user.online} />
            )}
          </div>
        </div>
      </button>
    </div>
  );
}
