import { Avatar } from "@/components/Avatar/Avatar";
import { ChatHeaderProps } from "@/components/Chat/MainBox/variants/ChatBox/ChatHeader/chatHeader.types";
import { LastSeen } from "@/components/LastSeen/LastSeen";
import { Status } from "@/components/Status/Status";

export function ChatHeader({ user }: Readonly<ChatHeaderProps>) {
  return (
    <div className="bg-dark-600 border-2 border-dark-150 rounded-2xl p-4 py-4 m-6 mb-0">
      <div className="flex">
        <Avatar className="mx-5" item={user} alt="Avatar" />
        <div className="flex flex-col justify-center">
          <h1 className="text-xl text-gray-100">{user.name}</h1>
          <div className="flex items-center">
            {user.online ? (
              <Status online={user.online} textStatus />
            ) : (
              <LastSeen lastSeen={user.lastSeen} online={user.online} />
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
