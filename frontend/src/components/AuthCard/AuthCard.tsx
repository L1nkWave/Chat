import React from "react";

import { AuthCardProps } from "@/components/AuthCard/authCard.types";

export function AuthCard({ children, className, ...props }: AuthCardProps) {
  return (
    <form
      className={`rounded-3xl bg-dark-550 h-fit w-[50vh] p-4 flex flex-col items-center pt-20 pb-10 ${className}`}
      {...props}
    >
      {children}
    </form>
  );
}
