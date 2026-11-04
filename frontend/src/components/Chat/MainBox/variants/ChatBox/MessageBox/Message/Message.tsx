import { format } from "date-fns";

import { ChatParams, MessageParams } from "@/api/http/contacts/contacts.types";
import { ChatType } from "@/api/socket/index.types";
import { Avatar } from "@/components/Avatar/Avatar";
import { FileMessage } from "@/components/Chat/MainBox/variants/ChatBox/MessageBox/Message/FileMessage";
import { DoubleCheckIcon } from "@/components/DoubleCheckIcon/DoubleCheckIcon";
import { Icon } from "@/components/Icon/Icon";
import { COLORS } from "@/constants/colors";
// eslint-disable-next-line import/named
import { formatDate } from "@/helpers/FormatDate/formatDate";
import { DateFormat } from "@/helpers/FormatDate/formatDate.types";
import { toDate } from "@/helpers/FormatDate/toDate";
import { useAppSelector } from "@/lib/hooks";

export type MessageProps = {
  message: MessageParams;
  nextMessage?: MessageParams;
  chat: ChatParams;
};

export enum MessageType {
  FILE = "FILE",
  MESSAGE = "MESSAGE",
}

const MEMBER_ACTION_LABELS: Record<string, string> = {
  ADD: "đã được thêm vào nhóm",
  JOIN: "đã tham gia nhóm",
  LEAVE: "đã rời nhóm",
  KICK: "đã bị mời ra khỏi nhóm",
};

export function Message({ chat, message, nextMessage }: Readonly<MessageProps>) {
  const { currentUser } = useAppSelector(state => state.user);
  const messageCreatedAt = format(toDate(message.createdAt), "H:mm");
  const date = toDate(message.createdAt);
  const nextDate = nextMessage && toDate(nextMessage.createdAt);
  const isCurrentUserAuthor = message.author.id === currentUser?.id;
  const isPreviousMessageAuthor = nextMessage?.author.id === message.author.id;
  const isFirstMessageFromUser = !isPreviousMessageAuthor;
  const shouldShowDate = !nextDate || nextDate.getDate() !== date.getDate();
  const isShowUser = chat.type === ChatType.GROUP && !isCurrentUserAuthor;
  const isTextMessage = message.action === MessageType.MESSAGE;
  const isFileMessage = message.action === MessageType.FILE;
  // Các tin thông báo thành viên (thêm, tham gia, rời, bị mời ra) không phải file
  const memberLabel = MEMBER_ACTION_LABELS[message.action];
  const memberName = (message as MessageParams & { name?: string }).name ?? message.author.name;

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
      <div className="flex w-full relative h-max z-40">
        {isShowUser && (
          <Avatar
            className="self-start max-h-[32px] mr-4"
            item={{ id: message.author.id, avatarPath: "/", avatarAvailable: true }}
            alt="Avatar"
            width={38}
            height={38}
          />
        )}

        <div className={`w-full flex flex-col -z-10 ${messageContainer}`}>
          <div
            className={`text-gray-200 px-6 py-2 whitespace-pre-wrap break-all ${messageCloudStyle} ${!isTextMessage && "bg-transparent py-0 my-0"}`}
          >
            {isShowUser && <p className="text-blue-100 text-start font-semibold">{message.author.name}</p>}
            {isTextMessage && message.text}
            {isFileMessage && <FileMessage message={message} />}
            {!isTextMessage && !isFileMessage && memberLabel && (
              <p className="text-center text-gray-400 text-sm">
                {memberName} {memberLabel}
              </p>
            )}
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
    </div>
  );
}
