import { UserItemProps } from "@/components/Chat/UserItem/userItem.types";
import { ChatItem } from "@/components/Chat/UserItem/variants/ChatItem";
import { ContactItem } from "@/components/Chat/UserItem/variants/ContactItem";

export function UserItem({ variant, chat, contact }: Readonly<UserItemProps>) {
  let chatItem = <ChatItem chat={chat} />;
  if (variant === "contact") {
    chatItem = <ContactItem contact={contact} />;
  }
  return (
    <button type="button" className="outline-none bg-dark-300 p-4 rounded-lg hover:bg-dark-200">
      {chatItem}
    </button>
  );
}
