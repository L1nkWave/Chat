"use client";

import { useFormik } from "formik";
import { jwtDecode } from "jwt-decode";
import { useRouter } from "next/navigation";
import React from "react";
import { toast } from "react-toastify";

import { signIn } from "@/api/http/auth/auth";
import { UserParams } from "@/api/http/contacts/contacts.types";
import { getUserById } from "@/api/http/user/user";
import { passwordInput, signInForm, signInValidationSchema, usernameInput } from "@/components/Auth/auth.config";
import { axiosErrorHandler, handleUsernameBlur } from "@/components/Auth/auth.utils";
import { CustomInput } from "@/components/CustomInput/CustomInput";
import { Form } from "@/components/Form/Form";
import { setAccessToken, setCurrentUser } from "@/lib/features/user/userSlice";
import { TokenParams } from "@/lib/features/user/userSlice.types";
import { useAppDispatch } from "@/lib/hooks";

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
        const { accessToken } = await signIn(values.username, values.password);
        dispatch(setAccessToken(accessToken));
        const decodedToken = jwtDecode<TokenParams>(accessToken);

        const userFromToken: UserParams = await getUserById(decodedToken["user-id"]);
        userFromToken.online = true;
        dispatch(setCurrentUser(userFromToken));

        toast.dismiss();
        router.push("/chat");
      } catch (error) {
        axiosErrorHandler(error);
      }
    },
  });

  return (
    <Form
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
        containerClassName="w-3/5 mb-6"
        innerContainerClassName="rounded-lg"
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
        containerClassName="w-3/5 mb-6"
        innerContainerClassName="rounded-lg"
        label={passwordInput.label}
        icon={passwordInput.icon}
        value={formik.values.password}
        onChange={formik.handleChange}
        onBlur={formik.handleBlur}
        error={formik.touched.password && formik.errors.password}
      />
    </Form>
  );
}
