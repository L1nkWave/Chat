import "./scrollList.styles.css";

import React, { ForwardedRef, useState } from "react";

import { ScrollListProps } from "@/components/ScrollList/scrollList.types";

export const ScrollList = React.forwardRef<HTMLDivElement, ScrollListProps>(
  ({ children, className, ...props }: Readonly<ScrollListProps>, ref: ForwardedRef<HTMLDivElement>) => {
    const [isMouseEntered, setIsMouseEntered] = useState(false);

    const handleMouseEnter = () => {
      setIsMouseEntered(true);
    };

    const handleMouseLeave = () => {
      setIsMouseEntered(false);
    };

    return (
      <div
        className={`flex flex-col h-full outline-none bg-dark-500 py-4 gap-2 overflow-y-scroll pr-4 pl-6 ${isMouseEntered ? "scroll-list__scrollbar" : "scroll-list__scrollbar-hidden"} ${className ?? ""}`}
        onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
        role="tablist"
        tabIndex={0}
        ref={ref}
        {...props}
      >
        {children}
      </div>
    );
  }
);
