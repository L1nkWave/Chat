import React, { PropsWithChildren } from "react";

export function SideBar({ children }: Readonly<PropsWithChildren>) {
  return (
    <div className="flex flex-col w-1/8 bg-dark-250 p-4 h-screen rounded-l-2xl gap-4 text-blue-500 text-lg">
      {children}
    </div>
  );
}
