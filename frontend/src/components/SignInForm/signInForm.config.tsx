import { FormIcon, LockIcon, PinIcon } from "@public/icons";
import React from "react";
import * as yup from "yup";

export const signInValidationSchema = yup.object({
  username: yup
    .string()
    .matches(
      /^@[_\-a-z]+$/,
      "Username must start with @ and can only contain _, -, and lowercase letters."
    )
    .required("Username is required"),
  password: yup
    .string()
    .min(3, "Password must be at least 3 characters.")
    .required("Password is required"),
});

export const authForm = {
  title: "Welcome back!",
  description: "Enter your email and password to sign in",
  buttonTitle: "Sign in",
  titleIcon: <FormIcon />,
};

export const usernameInput = {
  placeholder: "@emmtlor",
  name: "username",
  label: "Username",
  icon: <PinIcon />,
};

export const passwordInput = {
  placeholder: "●●●●●●●●●",
  type: "password",
  name: "password",
  label: "Password",
  icon: <LockIcon />,
};
