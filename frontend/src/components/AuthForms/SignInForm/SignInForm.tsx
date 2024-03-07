"use client";

import { useFormik } from "formik";
import { useRouter } from "next/navigation";
import React from "react";
import { toast } from "react-toastify";

import { signIn } from "@/api/http/auth/auth";
import { AuthForm } from "@/components/AuthForm/AuthForm";
import {
  passwordInput,
  signInForm,
  signInValidationSchema,
  usernameInput,
} from "@/components/AuthForms/authForms.config";
import { axiosErrorHandler, handleUsernameBlur } from "@/components/AuthForms/authForms.utils";
import { CustomInput } from "@/components/CustomInput/CustomInput";
import { setAccessToken } from "@/redux/features/auth/authSlice";
import { useAppDispatch } from "@/redux/hooks";

export function SignInForm() {
  const router = useRouter();
  const dispatch = useAppDispatch();
  const formik = useFormik({
    initialValues: {
      username: "",
      password: "",
    },
    validationSchema: signInValidationSchema,
    onSubmit: async values => {
      try {
        const data = await signIn(values.username, values.password);
        dispatch(setAccessToken(data.accessToken));
        toast.dismiss();
        router.push("/chat");
      } catch (error) {
        axiosErrorHandler(error);
      }
    },
  });

  return (
    <AuthForm
      onSubmit={formik.handleSubmit}
      titleIcon={signInForm.titleIcon}
      title={signInForm.title}
      description={signInForm.description}
      buttonTitle={signInForm.buttonTitle}
    >
      <CustomInput
        name={usernameInput.name}
        placeholder={usernameInput.placeholder}
        label={usernameInput.label}
        className="text-base"
        containerClassName="w-3/5"
        icon={usernameInput.icon}
        value={formik.values.username}
        onChange={formik.handleChange}
        onBlur={() => handleUsernameBlur(formik)}
        error={formik.touched.username && formik.errors.username}
      />
      <CustomInput
        name={passwordInput.name}
        placeholder={passwordInput.placeholder}
        type={passwordInput.type}
        className="text-base"
        containerClassName="w-3/5"
        label={passwordInput.label}
        icon={passwordInput.icon}
        value={formik.values.password}
        onChange={formik.handleChange}
        onBlur={formik.handleBlur}
        error={formik.touched.password && formik.errors.password}
      />
    </AuthForm>
  );
}
