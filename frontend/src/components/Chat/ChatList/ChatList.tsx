import React, { useState } from "react";

import { ChatItem } from "@/components/Chat/ChatItem/ChatItem";

export function ChatList() {
  const [isMouseEntered, setIsMouseEntered] = useState(true);
  const handleMouseEnter = () => {
    setIsMouseEntered(true);
  };
  const handleMouseLeave = () => {
    setIsMouseEntered(false);
  };
  return (
    <div
      className={`flex flex-col outline-none w-[55%] bg-dark-500 h-screen pl-8 pr-6 py-8 gap-2 overflow-y-scroll scrollbar-thin scrollbar-thumb-rounded-full scrollbar-track-rounded-full ${isMouseEntered ? "scrollbar-thumb-blue-700 scrollbar-track-dark-50/20" : ""}`}
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
  );
}
