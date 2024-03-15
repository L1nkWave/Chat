import React from "react";

import { MainBoxProps } from "@/components/Chat/MainBox/mainBox.types";

export function MainBox({ mainBoxVariant }: Readonly<MainBoxProps>) {
  if (mainBoxVariant !== "empty") {
    return null;
  }
  return (
    <div className="flex w-full bg-dark-550 p-2 h-screen items-center justify-center rounded-r-2xl text-gray-300">
      Choose chat room to start chatting
    </div>
  );
}
