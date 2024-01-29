import {
  LockOutlineIcon,
  PinOutlineIcon,
  UserFillIcon,
  UserOutlineIcon,
} from "@public/icons";
import * as yup from "yup";

export const validationSchema = yup.object({
  username: yup
    .string()
    .matches(
      /^@[_\-a-z]+$/,
      "Username must start with @ and can only contain _, -, and lowercase letters."
    )
    .min(3, "Username must be at least 3 characters.")
    .max(32, "Username must be at most 64 characters.")
    .required("Username is required"),
  fullName: yup
    .string()
    .min(3, "Full name must be at least 3 characters.")
    .max(64, "Full name must be at most 64 characters.")
    .required("Full name is required"),
  password: yup
    .string()
    .min(3, "Password must be at least 3 characters.")
    .max(64, "Password must be at most 64 characters.")
    .required("Password is required"),
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
