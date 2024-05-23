import { format } from "date-fns";

import { MessageParams } from "@/api/http/contacts/contacts.types";
import { DoubleCheckIcon } from "@/components/DoubleCheckIcon/DoubleCheckIcon";
import { Icon } from "@/components/Icon/Icon";
import { COLORS } from "@/constants/colors";
import { formatDate } from "@/helpers/FormatDate/formatDate";
import { DateFormat } from "@/helpers/FormatDate/formatDate.types";
import { useAppSelector } from "@/lib/hooks";

export type MessageProps = {
  message: MessageParams;
  nextMessage?: MessageParams;
};

export function Message({ message, nextMessage }: Readonly<MessageProps>) {
  const { currentUser } = useAppSelector(state => state.user);
  const messageCreatedAt = format(message.createdAt * 1000, "H:mm");
  const date = new Date(message.createdAt * 1000);
  const nextDate = nextMessage && new Date(nextMessage.createdAt * 1000);
  const isCurrentUserAuthor = message.author.id === currentUser?.id;
  const isPreviousMessageAuthor = nextMessage?.author.id === message.author.id;
  const isFirstMessageFromUser = !isPreviousMessageAuthor;
  const shouldShowDate = !nextDate || nextDate.getDate() !== date.getDate();

  let messageContainer: string;
  let messageCloudStyle: string;

  if (isCurrentUserAuthor) {
    messageContainer = "self-end items-end";
    messageCloudStyle = isFirstMessageFromUser ? "bg-dark-100 rounded-2xl rounded-tr-none" : "bg-dark-100 rounded-2xl";
  } else {
    messageContainer = "self-start items-start";
    messageCloudStyle = isFirstMessageFromUser ? "bg-dark-200 rounded-2xl rounded-tl-none" : "bg-dark-200 rounded-2xl";
  }

  return (
    <div className="w-full h-max relative flex flex-col">
      {shouldShowDate && (
        <span key={date.getTime()} className="self-end flex gap-2 text-gray-500 font-semibold">
          <Icon name="clock-outline" iconSize={18} />
          {formatDate(date.getTime(), DateFormat.MONTH_AND_DAY)}
          <Icon name="line-horizontal" iconSize={49} />
        </span>
      )}
      <div className={`w-3/5 flex flex-col -z-10 ${messageContainer}`}>
        <div className={`text-gray-200 px-6 py-2 whitespace-pre-wrap break-all ${messageCloudStyle}`}>
          {message.text}
        </div>
        <div className="flex gap-2">
          <span className="text-sm text-gray-400">{messageCreatedAt}</span>
          {isCurrentUserAuthor &&
            (message.sending ? (
              <Icon name="clock-outline" iconSize={15} color={COLORS.gray["500"]} />
            ) : (
              <DoubleCheckIcon
                iconSize={15}
                checkIconColor={COLORS.blue["200"]}
                cutCheckIconColor={message.isRead ? COLORS.blue["200"] : COLORS.dark["150"]}
              />
            ))}
        </div>
      </div>
    </div>
  );
}
