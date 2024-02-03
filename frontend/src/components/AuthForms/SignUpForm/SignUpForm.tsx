"use client";

import { useFormik } from "formik";
import { useRouter } from "next/navigation";
import React from "react";

import { signUp } from "@/api/auth/auth";
import { AuthForm } from "@/components/AuthForm/AuthForm";
import {
  fullNameInput,
  passwordInput,
  signUpForm,
  signUpValidationSchema,
  usernameInput,
} from "@/components/AuthForms/authForms.config";
import { handleUsernameBlur } from "@/components/AuthForms/authForms.utils";
import { CustomInput } from "@/components/CustomInput/CustomInput";

export function SignUpForm() {
  const router = useRouter();
  const formik = useFormik({
    initialValues: {
      username: "",
      fullName: "",
      password: "",
    },
    validationSchema: signUpValidationSchema,
    onSubmit: async values => {
      try {
        await signUp(values.fullName, values.username, values.password);
        router.push("/sign-in");
      } catch (error) {
        console.log(error);
      }
    },
  });

  return (
    <AuthForm
      onSubmit={formik.handleSubmit}
      titleIcon={signUpForm.titleIcon}
      title={signUpForm.title}
      description={signUpForm.description}
      buttonTitle={signUpForm.buttonTitle}
    >
      <CustomInput
        name={fullNameInput.name}
        placeholder={fullNameInput.placeholder}
        label={fullNameInput.label}
        className="text-base"
        containerClassName="w-3/5"
        icon={fullNameInput.icon}
        value={formik.values.fullName}
        onChange={formik.handleChange}
        error={formik.touched.fullName && formik.errors.fullName}
      />
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
