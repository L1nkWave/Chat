import "./scrollList.styles.ts.css";

import { PropsWithChildren, useState } from "react";

export function ScrollList({ children }: Readonly<PropsWithChildren>) {
  const [isMouseEntered, setIsMouseEntered] = useState(false);

  const handleMouseEnter = () => {
    setIsMouseEntered(true);
  };
  const handleMouseLeave = () => {
    setIsMouseEntered(false);
  };
  return (
    <div
      className={`flex flex-col h-full outline-none bg-dark-500 pl-8 pr-6 py-8 gap-2 overflow-y-auto ${isMouseEntered ? "scroll-list__scrollbar" : "scroll-list__scrollbar-hidden"}`}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      role="tablist"
      tabIndex={0}
    >
      {children}
    </div>
  );
}
