import { ChatBoxProps } from "@/components/Chat/MainBox/variants/ChatBox/chatBox.types";

export function ChatBox({ contact }: Readonly<ChatBoxProps>) {
  return <div>Chat Box {contact.user.name}</div>;
}
