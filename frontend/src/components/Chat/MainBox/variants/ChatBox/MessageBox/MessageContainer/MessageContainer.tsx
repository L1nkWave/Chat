import "./messageContainer.styles.css";

import React from "react";

import { Message } from "@/components/Chat/MainBox/variants/ChatBox/MessageBox/Message/Message";
import { MessageContainerProps } from "@/components/Chat/MainBox/variants/ChatBox/MessageBox/MessageContainer/messageContainer.types";
import { CustomButton } from "@/components/CustomButton/CustomButton";
import { Icon } from "@/components/Icon/Icon";
import { ScrollList } from "@/components/ScrollList/ScrollList";
import { COLORS } from "@/constants/colors";

export const MessageContainer = React.forwardRef<HTMLDivElement, MessageContainerProps>(
  ({ chat, messages, onScrollToBottomButtonClick: handleScrollToBottom, showScrollDownButton }, ref) => {
    return (
      <ScrollList
        ref={ref}
        className="bg-transparent flex-col-reverse w-full relative pb-36 z-20 message-container__scrollbar-height"
      >
        {messages.map((message, index) => {
          return <Message chat={chat} key={message.id} message={message} nextMessage={messages[index + 1]} />;
        })}
        {showScrollDownButton && (
          <CustomButton
            variant="transparent"
            type="button"
            onClick={handleScrollToBottom}
            className="fixed p-2 rounded-full left-[63%]"
          >
            <Icon name="angle-down" iconSize={24} color={COLORS.blue["500"]} />
          </CustomButton>
        )}
      </ScrollList>
    );
  }
);
