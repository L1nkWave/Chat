import { format } from "date-fns";

import { MessageParams } from "@/api/http/contacts/contacts.types";
import { DoubleCheckIcon } from "@/components/DoubleCheckIcon/DoubleCheckIcon";
import { COLORS } from "@/constants/colors";
import { useAppSelector } from "@/lib/hooks";

export type MessageProps = {
  message: MessageParams;
  previousMessage?: MessageParams;
};

export function Message({ message, previousMessage }: Readonly<MessageProps>) {
  const { currentUser } = useAppSelector(state => state.user);
  const messageCreatedAt = format(Date.now(), "H:mm");
  const isCurrentUserAuthor = message.author.id === currentUser?.id;
  const isPreviousMessageAuthor = previousMessage?.author.id === message.author?.id;
  let messageContainer: string;
  let messageCloudStyle: string;

  if (isCurrentUserAuthor) {
    messageContainer = "self-end items-end";
    messageCloudStyle = `bg-dark-100 ${isPreviousMessageAuthor ? "rounded-tr-2xl" : "rounded-tr-none"}`;
  } else {
    messageContainer = "self-start items-start";
    messageCloudStyle = `bg-dark-200 ${isPreviousMessageAuthor ? "rounded-tl-2xl" : "rounded-tl-none"}`;
  }

  if (message.author.id === previousMessage?.author.id) {
    messageCloudStyle += " rounded-tr-2xl rounded-tl-2xl";
  }
  return (
    <div className={`w-3/5 flex flex-col relative -z-10 ${messageContainer}`}>
      <div className={`text-gray-200 px-6 py-2 rounded-2xl ${messageCloudStyle}`}>{message.text}</div>
      <div className="flex gap-2">
        <span className="text-sm text-gray-400">{messageCreatedAt}</span>
        {isCurrentUserAuthor && (
          <DoubleCheckIcon
            iconSize={15}
            checkIconColor={COLORS.blue["200"]}
            cutCheckIconColor={message.isRead ? COLORS.blue["200"] : COLORS.dark["150"]}
          />
        )}
      </div>
    </div>
  );
}
