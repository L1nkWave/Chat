"use client";

import { useFormik } from "formik";
import React from "react";

import { AuthForm } from "@/components/AuthForm/AuthForm";
import { CustomInput } from "@/components/CustomInput/CustomInput";
import {
  authForm,
  passwordInput,
  signInValidationSchema,
  usernameInput,
} from "@/components/SignInForm/signInForm.config";

export function SignInForm() {
  const formik = useFormik({
    initialValues: {
      username: "",
      password: "",
    },
    validationSchema: signInValidationSchema,
    onSubmit: values => {
      // Handle form submission logic here
      console.log("Form submitted with values:", values);
    },
  });
  const handleUsernameBlur = () => {
    if (!formik.values.username.startsWith("@")) {
      formik.setFieldValue(usernameInput.name, `@${formik.values.username}`);
    }
    return formik.handleBlur(usernameInput.name);
  };
  return (
    <AuthForm
      onSubmit={formik.handleSubmit}
      titleIcon={authForm.titleIcon}
      title={authForm.title}
      description={authForm.description}
      buttonTitle={authForm.buttonTitle}
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
        onBlur={handleUsernameBlur}
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
