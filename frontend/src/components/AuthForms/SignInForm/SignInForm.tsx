"use client";

import { useFormik } from "formik";
import React from "react";

import { AuthForm } from "@/components/AuthForm/AuthForm";
import {
  passwordInput,
  signInForm,
  usernameInput,
  validationSchema,
} from "@/components/AuthForms/authForms.config";
import { handleUsernameBlur } from "@/components/AuthForms/authForms.utils";
import { CustomInput } from "@/components/CustomInput/CustomInput";

export function SignInForm() {
  const formik = useFormik({
    initialValues: {
      username: "",
      password: "",
    },
    validationSchema,
    onSubmit: values => {
      // Handle form submission logic here
      console.log("Form submitted with values:", values);
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
