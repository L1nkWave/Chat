import "./scrollList.styles.ts.css";

import { useState } from "react";

import { ScrollListProps } from "@/components/ScrollList/scrollList.types";

export function ScrollList({ children, className }: Readonly<ScrollListProps>) {
  const [isMouseEntered, setIsMouseEntered] = useState(false);

  const handleMouseEnter = () => {
    setIsMouseEntered(true);
  };
  const handleMouseLeave = () => {
    setIsMouseEntered(false);
  };
  return (
    <div
      className={`flex flex-col h-full outline-none bg-dark-500 pl-8 pr-6 py-8 gap-2 overflow-y-auto ${isMouseEntered ? "scroll-list__scrollbar" : "scroll-list__scrollbar-hidden"} ${className}`}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      role="tablist"
      tabIndex={0}
    >
      {children}
    </div>
  );
}
