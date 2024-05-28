import React, { useCallback, useLayoutEffect, useRef, useState } from "react";

import { ContactParams } from "@/api/http/contacts/contacts.types";
import { MESSAGE_CONTAINER, MESSAGE_INPUT } from "@/components/Chat/MainBox/variants/ChatBox/chatBox.config";
import { ChatBoxProps } from "@/components/Chat/MainBox/variants/ChatBox/chatBox.types";
import { ChatHeader } from "@/components/Chat/MainBox/variants/ChatBox/ChatHeader/ChatHeader";
import { MessageContainer } from "@/components/Chat/MainBox/variants/ChatBox/MessageBox/MessageContainer/MessageContainer";
import { MessageInput } from "@/components/Chat/MainBox/variants/ChatBox/MessageInput/MessageInput";
import { useAppSelector } from "@/lib/hooks";

export function ChatBox({
  onAddMemberClick: handleAddMemberClick,
  chat,
  contact,
  messages,
  onSendMessageClick,
  onChatHeaderClick,
  groupDetails,
  loadMessages,
  contacts,
}: Readonly<ChatBoxProps>) {
  const messageContainerRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const MAX_FILE_SIZE = 5 * 1024 * 1024 * 1024;
  const [initialTextAreaHeight, setInitialTextAreaHeight] = useState<number | undefined>(undefined);
  const [initialMessageContainerHeight, setInitialMessageContainerHeight] = useState<number | undefined>(undefined);
  const [showScrollDownButton, setShowScrollDownButton] = useState(false);
  const [message, setMessage] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [filePreviewUrl, setFilePreviewUrl] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const { currentUser } = useAppSelector(state => state.user);

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (event.target.files && event.target.files.length > 0) {
      const selectedFile = event.target.files[0];

      if (selectedFile.size > MAX_FILE_SIZE) {
        setErrorMessage("File size exceeds 5 GB limit.");
        return;
      }

      setFile(selectedFile);
      setErrorMessage(null);

      if (selectedFile.type.startsWith("image/")) {
        const previewUrl = URL.createObjectURL(selectedFile);
        setFilePreviewUrl(previewUrl);
      } else {
        setFilePreviewUrl(null);
      }
    }
  };

  const handleUploadClick = () => {
    document.getElementById("fileUpload")?.click();
  };

  const handleRemoveFile = () => {
    setFile(null);
    setFilePreviewUrl(null);
    const inputElement = document.getElementById("fileUpload") as HTMLInputElement;
    if (inputElement) {
      inputElement.value = "";
    }
  };

  const scrollToBottom = (behavior?: ScrollBehavior) => {
    if (messageContainerRef.current) {
      console.log(messageContainerRef.current.scrollHeight);
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
    if (onSendMessageClick && currentUser) {
      onSendMessageClick(currentUser, message.trim(), file);
      setMessage("");
      setFile(null);
      setFilePreviewUrl(null);
      setErrorMessage(null);
      if (textareaRef.current) {
        textareaRef.current.style.height = MESSAGE_INPUT.DEFAULT_TEXTAREA_HEIGHT;
      }
      setTimeout(() => {
        scrollToBottom();
      }, 0);
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

  const handleLoad = () => {
    scrollToBottom("auto");
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
        <ChatHeader
          groupDetails={groupDetails}
          contact={contact}
          chat={chat}
          onChatHeaderClick={handleHeaderClick}
          onAddMemberClick={handleAddMemberClick}
          contacts={contacts}
        />
        <MessageContainer
          onLoad={handleLoad}
          chat={chat}
          ref={messageContainerRef}
          messages={messages}
          onScrollToBottomButtonClick={handleScrollToBottomButtonClick}
          showScrollDownButton={showScrollDownButton}
        />
        <MessageInput
          file={file}
          filePreviewUrl={filePreviewUrl}
          errorMessage={errorMessage}
          onFileChange={handleFileChange}
          onRemoveFile={handleRemoveFile}
          onUploadClick={handleUploadClick}
          ref={textareaRef}
          onTextAreaChange={handleTextAreaChange}
          onTextAreaKeyDown={handleTextareaKeyDown}
          onSendMessageClick={handleSendMessageClick}
          message={message}
        />
      </div>
    </div>
  );
}
