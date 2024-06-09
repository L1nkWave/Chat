import { useCallback, useEffect, useRef, useState } from "react";
import { toast } from "react-toastify";

import { getContacts } from "@/api/http/contacts/contacts";
import { ChatParams, ContactParams, GroupChatDetails, GroupRole } from "@/api/http/contacts/contacts.types";
import { Avatar } from "@/components/Avatar/Avatar";
import { ContactsMap } from "@/components/Chat/InteractiveList/interactiveList.types";
import { InfoTextBox } from "@/components/Chat/MainBox/variants/UserInfoBox/InfoBox/InfoTextBox";
import { InfoIconShape } from "@/components/Chat/MainBox/variants/UserInfoBox/InfoIconShape/InfoIconShape";
import { CustomButton } from "@/components/CustomButton/CustomButton";
import { GroupDetailsButton } from "@/components/GroupDetailsModal/GroupDetailsButton/GroupDetailsButton";
import { Icon } from "@/components/Icon/Icon";
import { ScrollList } from "@/components/ScrollList/ScrollList";
import { COLORS } from "@/constants/colors";
import { UserStatus } from "@/lib/features/user/userSlice.types";
import { useAppSelector } from "@/lib/hooks";

export type GroupDetailsModalProps = {
  onClose: () => void;
  groupDetails: GroupChatDetails;
  chat: ChatParams;
  isOpen: boolean;
  defaultGroupAvatar: string;
  contacts: ContactsMap;
  onEditGroupClick?: () => void;
  onClearHistoryClick?: () => void;
  onLeaveGroupClick?: () => void;
  onAddMemberClick?: (currentContact: ContactParams) => void;
  onDeleteChat?: () => void;
};

export function GroupDetailsModal({
  onClose: handleClose,
  onEditGroupClick: handleEditGroupClick,
  onClearHistoryClick: handleClearHistoryClick,
  onLeaveGroupClick: handleLeaveGroupClick,
  onAddMemberClick,
  defaultGroupAvatar,
  onDeleteChat: handleDeleteChat,
  chat,
  groupDetails,
  isOpen,
}: Readonly<GroupDetailsModalProps>) {
  const [isAddButtonClicked, setIsAddButtonClicked] = useState(false);
  const [contacts, setContacts] = useState<ContactsMap>(new Map());

  const fetchContacts = useCallback(async (search?: string, offset?: number, limit?: number) => {
    const fetchedContacts = await getContacts(search, limit, offset);
    setContacts(prevContacts => {
      const updatedContacts = new Map(prevContacts);
      fetchedContacts.forEach((fetchedContact, key) => {
        updatedContacts.set(key, fetchedContact);
      });
      return updatedContacts;
    });
  }, []);

  const [loading, setLoading] = useState(false);
  const scrollListRef = useRef<HTMLDivElement>(null);

  const handleScroll = useCallback(() => {
    const scrollHeight = scrollListRef.current?.scrollHeight ?? 0;
    const scrollTop = scrollListRef.current?.scrollTop ?? 0;
    const clientHeight = scrollListRef.current?.clientHeight ?? 0;

    const distanceFromBottom = scrollHeight - scrollTop - clientHeight;
    const loadThreshold = 450;

    if (distanceFromBottom < loadThreshold && !loading) {
      setLoading(true);
      fetchContacts("", contacts.size);
    }
  }, [contacts, fetchContacts, loading]);

  useEffect(() => {
    fetchContacts();
  }, []);
  useEffect(() => {
    const scrollList = scrollListRef.current;
    if (scrollList) {
      scrollList.addEventListener("scroll", handleScroll);
    }
    return () => {
      if (scrollList) {
        scrollList.removeEventListener("scroll", handleScroll);
      }
    };
  }, [handleScroll, loading]);

  const toggleAddMemberButtonClicked = () => {
    setIsAddButtonClicked(!isAddButtonClicked);
  };

  const handleAddMemberClick = (currentContact: ContactParams) => {
    if (onAddMemberClick) {
      onAddMemberClick(currentContact);
    }
  };

  const handleRemoveMemberClick = () => {
    toast.warn("Coming soon!");
  };

  const { currentUser } = useAppSelector(state => state.user);
  if (!isOpen || !currentUser) {
    return null;
  }

  return (
    <div className="relative z-50" aria-labelledby="modal-title" aria-modal="true">
      <div className="fixed inset-0 bg-dark-500 bg-opacity-75 transition-opacity" />
      <div className="fixed inset-0 z-10 w-screen overflow-y-auto">
        <div className="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
          <div className="max-w-2xl relative flex flex-col transform overflow-hidden rounded-2xl border dark:border-dark-150 dark:bg-dark-550 text-left shadow-xl transition-all my-8">
            <div className="dark:bg-dark-550 px-10 pt-10 sm:flex gap-12 items-center justify-between">
              <div className="flex items-center gap-12">
                <CustomButton variant="outline" onClick={handleClose} className="px-2 py-2">
                  <Icon name="left-angle" iconSize={28} color={COLORS.blue["200"]} />
                </CustomButton>
                <div className="flex">
                  <Avatar
                    item={chat}
                    isGroupAvatar
                    alt="Group Avatar"
                    className="mr-4"
                    width={64}
                    height={64}
                    defaultAvatar={defaultGroupAvatar}
                  />
                  <div>
                    <h1 className="text-xl text-gray-100 text-start flex gap-1.5">
                      <Icon iconSize={22} name="lock-outline" color={COLORS.blue["500"]} /> {chat.name}
                    </h1>
                    <p className="text-gray-400 text-base">
                      {groupDetails?.members.size} {groupDetails?.members.size === 1 ? "member" : "members"} /{" "}
                      {groupDetails?.membersLimit}
                    </p>
                  </div>
                </div>
              </div>

              {groupDetails.members.get(currentUser.id)?.role === GroupRole.ADMIN && (
                <CustomButton variant="outline" onClick={handleDeleteChat} className="px-2 py-2">
                  <Icon name="trash-bucket-outline" iconSize={28} color={COLORS.blue["200"]} />
                </CustomButton>
              )}
            </div>
            <div className="py-6 px-8 flex flex-col gap-6">
              <div>
                <InfoTextBox>
                  <InfoIconShape icon="list-outline" />
                  {groupDetails.description}
                </InfoTextBox>
              </div>
              <div className="bg-dark-500 rounded-xl">
                {groupDetails.members.get(currentUser.id)?.role === GroupRole.ADMIN && (
                  <>
                    <GroupDetailsButton
                      onClick={handleEditGroupClick}
                      title="Edit"
                      className="rounded-t-xl"
                      description="Change chat name, description, etc."
                      iconName="edit-outline"
                    />
                    <GroupDetailsButton
                      onClick={handleClearHistoryClick}
                      title="Clear history"
                      description="Remove all chat messages without recovering"
                      iconName="trash-bucket-outline"
                    />
                  </>
                )}
                <GroupDetailsButton
                  onClick={handleLeaveGroupClick}
                  title="Leave"
                  description="Gracefully exit a chat without any hassle"
                  iconName="exit-outline"
                  className={`rounded-b-xl ${groupDetails.members.get(currentUser.id)?.role !== GroupRole.ADMIN && "rounded-t-xl"}`}
                />
              </div>
              <div className="bg-dark-500 rounded-xl">
                <div className="flex justify-between items-center py-4 px-6">
                  <div className="flex text-blue-400 gap-4">
                    <Icon name="users-outline" iconSize={24} />
                    <span className="font-semibold text-lg">{groupDetails.members.size} Members</span>
                  </div>
                  <div>
                    {groupDetails.members.get(currentUser.id)?.role === GroupRole.ADMIN && (
                      <CustomButton onClick={toggleAddMemberButtonClicked} variant="transparent">
                        {!isAddButtonClicked ? (
                          <Icon name="add-circle-outline" iconSize={28} color={COLORS.blue["200"]} />
                        ) : (
                          <Icon name="left-angle" iconSize={28} color={COLORS.blue["200"]} />
                        )}
                      </CustomButton>
                    )}
                  </div>
                </div>
                <hr className="mx-6 border-dark-150" />
                <ScrollList ref={scrollListRef} className="px-0 py-0 mx-0 max-h-64 gap-0">
                  {!isAddButtonClicked
                    ? Array.from(groupDetails.members.values()).map(member => (
                        <div key={member.id} className="flex items-center py-1">
                          <Avatar
                            width={64}
                            height={64}
                            online={member.details.online}
                            item={member}
                            alt="Member Avatar"
                            className="mr-4"
                          />
                          <div className="flex flex-col">
                            <div className="flex">
                              <span className="text-lg mr-2">{member.details.name}</span>
                              {member.role === GroupRole.ADMIN && (
                                <Icon name="crown-outline" iconSize={22} color={COLORS.blue["200"]} />
                              )}
                            </div>
                            <span className="text-gray-400">
                              {member.details.online ? UserStatus.ONLINE : UserStatus.OFFLINE}
                            </span>
                          </div>
                        </div>
                      ))
                    : Array.from(contacts.values()).map(contact => (
                        <div key={contact.user.id} className="py-1 flex items-center justify-between">
                          <div className="flex items-center">
                            <Avatar
                              width={64}
                              height={64}
                              online={contact.user.online}
                              item={contact.user}
                              alt="Member Avatar"
                              className="mr-4"
                            />
                            <div className="flex flex-col">
                              <div className="flex">
                                <span className="text-lg mr-2">{contact.user.name}</span>
                              </div>
                              <span className="text-gray-400">
                                {contact.user.online ? UserStatus.ONLINE : UserStatus.OFFLINE}
                              </span>
                            </div>
                          </div>
                          {!groupDetails.members.has(contact.user.id) ? (
                            <CustomButton onClick={() => handleAddMemberClick(contact)} variant="transparent">
                              <Icon name="add-circle-outline" iconSize={28} color={COLORS.blue["200"]} />
                            </CustomButton>
                          ) : (
                            <CustomButton onClick={() => handleRemoveMemberClick()} variant="transparent">
                              <Icon name="minus-circle-outline" iconSize={28} color={COLORS.blue["200"]} />
                            </CustomButton>
                          )}
                        </div>
                      ))}
                </ScrollList>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
