import React from "react";

import { AuthCardProps } from "@/components/AuthCard/authCard.types";

export function AuthCard({ children, className, ...props }: AuthCardProps) {
  return (
    <form
      className={`rounded-3xl bg-dark-550 h-4/5 w-[50vh] p-4 flex flex-col items-center pt-20 ${className}`}
      {...props}
    >
      {children}
    </form>
  );
}
