import { useState } from "react";
import { toast } from "react-toastify";

import { ContactParams } from "@/api/http/contacts/contacts.types";
import { ChatType } from "@/api/socket/index.types";
import { Avatar } from "@/components/Avatar/Avatar";
import { ChatHeaderProps } from "@/components/Chat/MainBox/variants/ChatBox/ChatHeader/chatHeader.types";
import { GroupDetailsModal } from "@/components/GroupDetailsModal/GroupDetailsModal";
import { LastSeen } from "@/components/LastSeen/LastSeen";
import { Status } from "@/components/Status/Status";

export function ChatHeader({
  contact,
  chat,
  onChatHeaderClick,
  groupDetails,
  onAddMemberClick,
  contacts,
}: Readonly<ChatHeaderProps>) {
  const [isGroupDetailsOpen, setIsGroupDetailsOpen] = useState(false);
  const defaultGroupAvatar = "/avatars/group.png";
  const handleGroupDetailsClose = () => {
    setIsGroupDetailsOpen(false);
  };

  const handleAddMemberClick = (currentContact: ContactParams) => {
    if (onAddMemberClick) {
      onAddMemberClick(chat, currentContact);
    }
  };

  const handleClick = () => {
    if (chat.type === ChatType.GROUP) {
      setIsGroupDetailsOpen(true);
    }
    if (chat.type === ChatType.DUO && onChatHeaderClick && contact) {
      onChatHeaderClick(contact);
    }
  };

  const handleEditGroupClick = () => {
    toast.warn("Coming soon!");
  };
  const handleClearHistoryClick = () => {
    toast.warn("Coming soon!");
  };

  return (
    <div className="bg-dark-600 border-2 border-dark-150 rounded-2xl p-4 py-4 m-6 mb-0">
      {groupDetails && (
        <GroupDetailsModal
          contacts={contacts}
          defaultGroupAvatar={defaultGroupAvatar}
          isOpen={isGroupDetailsOpen}
          chat={chat}
          groupDetails={groupDetails}
          onClose={handleGroupDetailsClose}
          onEditGroupClick={handleEditGroupClick}
          onClearHistoryClick={handleClearHistoryClick}
          onLeaveGroupClick={handleClearHistoryClick}
          onAddMemberClick={handleAddMemberClick}
          onDeleteChat={handleClearHistoryClick}
        />
      )}
      <button type="button" className="flex rounded-2xl items-center" onClick={handleClick}>
        {contact ? (
          <Avatar className="mx-5" item={contact.user} alt="Avatar" width={64} height={64} />
        ) : (
          <Avatar className="mx-5" item={chat} defaultAvatar={defaultGroupAvatar} alt="Avatar" width={64} height={64} />
        )}

        <div className="flex flex-col justify-center">
          <h1 className="text-xl text-gray-100 text-start">{chat.name}</h1>
          {chat.type === ChatType.DUO ? (
            contact && (
              <div className="flex items-center">
                {contact.user.online ? (
                  <Status online={contact.user.online} textStatus />
                ) : (
                  <LastSeen lastSeen={contact.user.lastSeen} online={contact.user.online} />
                )}
              </div>
            )
          ) : (
            <div className="flex items-center">
              <p className="text-gray-400 text-base">
                {groupDetails?.members.size} {groupDetails?.members.size === 1 ? "member" : "members"}
              </p>
            </div>
          )}
        </div>
      </button>
    </div>
  );
}
