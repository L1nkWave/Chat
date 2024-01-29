import React from "react";

import { AuthCardProps } from "@/components/AuthCard/authCard.types";

export function AuthCard({ children, className, ...props }: AuthCardProps) {
  return (
    <form
      className={`rounded-3xl h-fit w-[50vh] p-4 flex flex-col items-center pt-20 pb-10 shadow-2xl border border-gray-100 dark:bg-dark-550 dark:border-0 dark:shadow-none ${className}`}
      {...props}
    >
      {children}
    </form>
  );
}
