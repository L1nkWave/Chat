import { SignOutCircleIcon } from "@public/icons";
import React, { useState } from "react";

import { ChatItem } from "@/components/Chat/ChatItem/ChatItem";
import { CustomButton } from "@/components/CustomButton/CustomButton";

export function ChatList() {
  const [isMouseEntered, setIsMouseEntered] = useState(false);
  const handleMouseEnter = () => {
    setIsMouseEntered(true);
  };
  const handleMouseLeave = () => {
    setIsMouseEntered(false);
  };
  return (
    <div className="h-screen flex flex-col w-[55%]">
      <div
        className={`flex flex-col outline-none bg-dark-500 pl-8 pr-6 py-8 gap-2 overflow-y-scroll scrollbar-thin scrollbar-thumb-rounded-full scrollbar-track-rounded-full ${isMouseEntered ? "scrollbar-thumb-blue-700 scrollbar-track-dark-50/20" : ""}`}
        onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
        role="tablist"
        tabIndex={0}
      >
        {[...Array(50)].map((_, index) => (
          // eslint-disable-next-line react/no-array-index-key
          <ChatItem key={index} />
        ))}
      </div>
      <div className="bg-dark-400 px-8 py-4 left-0 bottom-0 w-full flex justify-between items-center">
        <div className="flex items-center gap-2">
          <div className="w-12 h-12 bg-gray-400 rounded-full" />
          <div className="flex flex-col gap-0">
            <p className="text-lg">Artem Magei</p>
            <div className="flex items-center text-gray-300">
              <div className="bg-green rounded-full w-2 h-2 mr-1" /> Active
            </div>
          </div>
        </div>
        <CustomButton
          className="p-1.5 text-blue-300 dark:bg-dark-250 w-10 h-10"
          iconSize={32}
          variant="square"
          icon={<SignOutCircleIcon />}
        />
      </div>
    </div>
  );
}
