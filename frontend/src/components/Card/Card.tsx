import React from "react";

import { CardProps } from "@/components/Card/card.types";

export function Card({ children, className, ...props }: CardProps) {
  return (
    <form
      className={`rounded-3xl h-fit w-[30rem] p-4 flex flex-col items-center py-20 shadow-2xl border border-gray-100 dark:bg-dark-550 dark:border-0 dark:shadow-none ${className}`}
      {...props}
    >
      {children}
    </form>
  );
}
