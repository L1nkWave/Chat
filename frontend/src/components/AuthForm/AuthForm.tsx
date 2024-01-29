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
      <div className="w-[100px] bg-dark-150 rounded-full p-4 text-2xl text-gray-100">
        {titleIcon}
      </div>
      <h2 className="text-white text-2xl">{title}</h2>
      <span className="text-gray-300">{description}</span>
      <div className="mt-10 flex flex-col w-full items-center">{children}</div>
      <CustomButton type="submit" className="mt-10 w-3/5" variant="flattened">
        {buttonTitle}
      </CustomButton>
    </AuthCard>
  );
}
