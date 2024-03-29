import { Avatar } from "@/components/Avatar/Avatar";
import { ChatItemProps } from "@/components/Chat/UserItem/userItem.types";

export function ChatItem({ chat }: Readonly<ChatItemProps>) {
  if (!chat) {
    return null;
  }
  return (
    <div className="flex flex-row justify-between items-center">
      <div className="flex flex-row">
        <div className="w-14 h-14">
          <Avatar item={chat} alt="User Avatar" />
        </div>
        <div className="flex flex-col ml-4 justify-center items-start">
          <span className="text-lg">{chat.name}</span>
          <p className="text-gray-300"> {chat.lastMessage} </p>
        </div>
      </div>
      <div>
        <p className="text-gray-300 text-sm">16:50</p>
        <div className="flex justify-center items-center bg-blue-300 rounded-lg mt-1">
          {chat.unreadMessagesCount > 0 && <p className="text-white text-xs p-1">{chat.unreadMessagesCount}</p>}
        </div>
      </div>
    </div>
  );
}
