import React, { useCallback, useLayoutEffect, useRef, useState } from "react";

import { ContactParams } from "@/api/http/contacts/contacts.types";
import { MESSAGE_CONTAINER, MESSAGE_INPUT } from "@/components/Chat/MainBox/variants/ChatBox/chatBox.config";
import { ChatBoxProps } from "@/components/Chat/MainBox/variants/ChatBox/chatBox.types";
import { ChatHeader } from "@/components/Chat/MainBox/variants/ChatBox/ChatHeader/ChatHeader";
import { MessageContainer } from "@/components/Chat/MainBox/variants/ChatBox/MessageBox/MessageContainer/MessageContainer";
import { MessageInput } from "@/components/Chat/MainBox/variants/ChatBox/MessageInput/MessageInput";
import { useAppSelector } from "@/lib/hooks";

export function ChatBox({
  contact,
  messages,
  onSendMessageClick,
  onChatHeaderClick,
  loadMessages,
}: Readonly<ChatBoxProps>) {
  const messageContainerRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const [initialTextAreaHeight, setInitialTextAreaHeight] = useState<number | undefined>(undefined);
  const [initialMessageContainerHeight, setInitialMessageContainerHeight] = useState<number | undefined>(undefined);
  const [showScrollDownButton, setShowScrollDownButton] = useState(false);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  const { currentUser } = useAppSelector(state => state.user);

  const scrollToBottom = (behavior?: ScrollBehavior) => {
    if (messageContainerRef.current) {
      messageContainerRef.current.scrollTo({
        top: messageContainerRef.current.scrollHeight,
        behavior,
      });
    }
  };

  const handleHeaderClick = (headerContact: ContactParams) => {
    if (onChatHeaderClick) {
      onChatHeaderClick(headerContact);
    }
  };

  const handleSendMessageClick = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (message && onSendMessageClick && currentUser) {
      if (message.trim().length === 0) return;
      onSendMessageClick(currentUser, message.trim());
      setMessage("");
      if (textareaRef.current) {
        textareaRef.current.style.height = MESSAGE_INPUT.DEFAULT_TEXTAREA_HEIGHT;
      }
      scrollToBottom();
    }
  };

  const handleTextareaKeyDown = (event: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      handleSendMessageClick(event as unknown as React.FormEvent<HTMLFormElement>);
    }
  };

  const handleTextAreaChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => setMessage(event.target.value);

  const handleScroll = useCallback(() => {
    if (messageContainerRef.current) {
      const { scrollTop, scrollHeight, clientHeight } = messageContainerRef.current;

      if (scrollTop < MESSAGE_CONTAINER.SHOW_SCROLL_DOWN_BUTTON_THRESHOLD_HEIGHT) {
        setShowScrollDownButton(true);
      } else {
        setShowScrollDownButton(false);
      }

      const distanceFromTop = scrollTop + scrollHeight - clientHeight;
      const loadThreshold = 500;
      if (distanceFromTop < loadThreshold) {
        setLoading(true);
        loadMessages(messages.length);
      }
    }
  }, [messages, loadMessages]);

  useLayoutEffect(() => {
    const scrollList = messageContainerRef.current;
    if (scrollList) {
      scrollList.addEventListener("scroll", handleScroll);
    }
    return () => {
      if (scrollList) {
        scrollList.removeEventListener("scroll", handleScroll);
      }
    };
  }, [handleScroll]);

  const handleScrollToBottomButtonClick = () => {
    scrollToBottom("smooth");
  };

  useLayoutEffect(() => {
    if (textareaRef.current && messageContainerRef.current) {
      let initTextAreaHeight: number = initialTextAreaHeight as number;
      let initMessageContainerHeight: number = initialMessageContainerHeight as number;

      if (!initialTextAreaHeight || !initialMessageContainerHeight) {
        initTextAreaHeight = textareaRef.current.scrollHeight;
        initMessageContainerHeight = messageContainerRef.current.offsetHeight;

        setInitialMessageContainerHeight(initMessageContainerHeight);
        setInitialTextAreaHeight(initTextAreaHeight);
      }

      textareaRef.current.style.height = `${initTextAreaHeight}px`;
      textareaRef.current.style.height =
        textareaRef.current.scrollHeight < MESSAGE_INPUT.MAX_TEXTAREA_HEIGHT
          ? `${textareaRef.current.scrollHeight}px`
          : `${MESSAGE_INPUT.MAX_TEXTAREA_HEIGHT}px`;

      const currentTextAreaHeight = textareaRef.current.offsetHeight;
      const newMessageContainerHeight = initMessageContainerHeight - currentTextAreaHeight + initTextAreaHeight;

      messageContainerRef.current.style.height = `${initMessageContainerHeight}px`;
      messageContainerRef.current.style.height = `${newMessageContainerHeight}px`;
    }
  }, [initialMessageContainerHeight, initialTextAreaHeight, message]);

  return (
    <div className="relative h-full w-full flex flex-col justify-between">
      <div className="flex flex-col w-full h-full">
        <ChatHeader contact={contact} onChatHeaderClick={handleHeaderClick} />
        <MessageContainer
          ref={messageContainerRef}
          messages={messages}
          onScrollToBottomButtonClick={handleScrollToBottomButtonClick}
          showScrollDownButton={showScrollDownButton}
        />
        <MessageInput
          ref={textareaRef}
          onTextAreaChange={handleTextAreaChange}
          onTextAreaKeyDown={handleTextareaKeyDown}
          onSendMessageClick={handleSendMessageClick}
          message={message}
        />
        {loading && <div>Loading more messages...</div>}
      </div>
    </div>
  );
}
