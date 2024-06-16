import Image from "next/image";
import React from "react";

import { CustomButton } from "@/components/CustomButton/CustomButton";
import { Icon } from "@/components/Icon/Icon";
import { COLORS } from "@/constants/colors";

export type MessageInputProps = {
  onTextAreaChange: React.ChangeEventHandler<HTMLTextAreaElement>;
  onSendMessageClick: React.FormEventHandler<HTMLFormElement>;
  onTextAreaKeyDown: React.KeyboardEventHandler<HTMLTextAreaElement>;
  onFileChange: React.ChangeEventHandler<HTMLInputElement>;
  onRemoveFile: () => void;
  onUploadClick: () => void;
  message: string;
  errorMessage: string | null;
  file: File | null;
  filePreviewUrl: string | null;
};

export const MessageInput = React.forwardRef<HTMLTextAreaElement, MessageInputProps>(
  (
    {
      message,
      onTextAreaChange: handleTextAreaChange,
      onSendMessageClick: handleSendMessageClick,
      onTextAreaKeyDown: handleTextareaKeyDown,
      onUploadClick: handleUploadClick,
      onFileChange: handleFileChange,
      onRemoveFile: handleRemoveFile,
      errorMessage,
      file,
      filePreviewUrl,
    },
    ref
  ) => {
    return (
      <form
        onSubmit={handleSendMessageClick}
        className="z-20 absolute w-full bottom-0 overflow-hidden bg-transparent flex flex-col-reverse justify-between px-16 pb-10 items-start"
      >
        <div className="flex w-full justify-between">
          <div className="dark:bg-dark-200 rounded-xl flex flex-row w-3/4 min-h-14 items-center justify-center">
            <CustomButton variant="transparent" onClick={handleUploadClick}>
              <Icon name="folder-outline" className="ml-4" color={COLORS.blue["200"]} />
            </CustomButton>
            <input type="file" id="fileUpload" style={{ display: "none" }} onChange={handleFileChange} />
            <textarea
              ref={ref}
              value={message}
              onKeyDown={handleTextareaKeyDown}
              onChange={handleTextAreaChange}
              id="chat"
              rows={1}
              className="block mx-4 py-2 h-fit my-2 w-full resize-none text-lg rounded-lg dark:bg-dark-200 outline-none dark:placeholder-blue-300 dark:text-gray-200"
              placeholder="Type message..."
            />
          </div>
          <CustomButton type="submit" variant="square" className="w-1/6 py-2 h-14 !bg-blue-100 !dark:bg-blue-100">
            <Icon name="send-outline" iconSize={38} color={COLORS.white} />
          </CustomButton>

          <div className="-z-10 absolute inset-x-0 bottom-0 h-full bg-dark-550 blur opacity-98 rounded-xl" />
          <div className="-z-10 absolute inset-x-0 bottom-0 h-5/6 bg-dark-550" />
        </div>
        {errorMessage && (
          <div className="absolute bottom-16 left-16 p-2 bg-red-500 text-white rounded-md shadow-md">
            {errorMessage}
          </div>
        )}
        {file && !errorMessage && (
          <div className="z-30 bottom-16 left-16 p-2 bg-white dark:bg-dark-300 rounded-md shadow-md flex items-start">
            {filePreviewUrl ? (
              <div className="relative max-h-60">
                <Image
                  width={120}
                  height={120}
                  src={filePreviewUrl}
                  alt="File preview"
                  className="rounded-md object-cover"
                />
                <CustomButton
                  variant="transparent"
                  className="absolute top-0 right-0 bg-dark-50 rounded-full opacity-40 hover:opacity-100"
                >
                  <Icon name="close-outline" onClick={handleRemoveFile} color={COLORS.blue["100"]} />
                </CustomButton>
              </div>
            ) : (
              <p className="text-sm text-gray-800 dark:text-gray-200 flex gap-4 items-center">
                {file.name}
                <CustomButton variant="transparent" className="relative top-0 right-0 bg-dark-50 rounded-full">
                  <Icon name="close-outline" onClick={handleRemoveFile} color={COLORS.blue["100"]} />
                </CustomButton>
              </p>
            )}
          </div>
        )}
      </form>
    );
  }
);
