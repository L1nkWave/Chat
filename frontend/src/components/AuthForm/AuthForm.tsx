import React from "react";

import { AuthCard } from "@/components/AuthCard/AuthCard";
import { AuthFormProps } from "@/components/AuthForm/authForm.types";
import { CustomButton } from "@/components/CustomButton/CustomButton";

export function AuthForm({
  children,
  titleIcon,
  title,
  description,
  buttonTitle,
  onSubmit: handleSubmit,
}: Readonly<AuthFormProps>) {
  return (
    <AuthCard onSubmit={handleSubmit}>
      <div className="w-[100px] rounded-full p-4 text-2xl bg-gray-100 text-dark-50 dark:text-gray-100 dark:bg-dark-150">
        {titleIcon}
      </div>
      <h2 className="text-dark-500 text-2xl dark:text-gray-100">{title}</h2>
      <span className="text-gray-500 dark:text-gray-300">{description}</span>
      <div className="mt-10 flex flex-col w-full items-center">{children}</div>
      <CustomButton type="submit" className="mt-10 w-3/5" variant="flattened">
        {buttonTitle}
      </CustomButton>
    </AuthCard>
  );
}
