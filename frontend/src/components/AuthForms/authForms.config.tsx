import {
  LockOutlineIcon,
  PinOutlineIcon,
  UserFillIcon,
  UserOutlineIcon,
} from "@public/icons";
import * as yup from "yup";

export const commonValidationSchema = {
  username: yup
    .string()
    .matches(
      /^@[_\-a-z0-9]+$/,
      "Username can contain only _, -, numbers and lowercase letters."
    )
    .min(3, "Username must be at least 3 characters.")
    .max(32, "Username must be at most 64 characters.")
    .required("Username is required"),
  password: yup
    .string()
    .min(3, "Password must be at least 3 characters.")
    .max(64, "Password must be at most 64 characters.")
    .required("Password is required"),
};

export const signInValidationSchema = yup.object({
  ...commonValidationSchema,
});

export const signUpValidationSchema = yup.object({
  ...commonValidationSchema,
  fullName: yup
    .string()
    .min(3, "Full name must be at least 3 characters.")
    .max(64, "Full name must be at most 64 characters.")
    .required("Full name is required"),
});

export const signInForm = {
  title: "Welcome back!",
  description: "Enter your email and password to sign in",
  titleIcon: <UserFillIcon />,
  buttonTitle: "Sign in",
};

export const signUpForm = {
  title: "Create an account",
  description: "Enter your details to create an account",
  titleIcon: <UserFillIcon />,
  buttonTitle: "Sign up",
};

export const fullNameInput = {
  placeholder: "Emma Taylor",
  name: "fullName",
  label: "Full name",
  icon: <UserOutlineIcon />,
};

export const usernameInput = {
  placeholder: "@emmtlor",
  name: "username",
  label: "Username",
  icon: <PinOutlineIcon />,
};

export const passwordInput = {
  placeholder: "●●●●●●●●●",
  type: "password",
  name: "password",
  label: "Password",
  icon: <LockOutlineIcon />,
};

export const messages = {
  DEFAULT_ERROR_MESSAGE: "An error occurred. Please try again.",
  SIGN_UP_SUCCESS_MESSAGE: "Account created successfully! You can sign in now.",
};
