import { FormikValues } from "formik";

import { usernameInput } from "@/components/AuthForms/authForms.config";

export const handleUsernameBlur = (formik: FormikValues) => {
  if (!formik.values.username.startsWith("@")) {
    formik.setFieldValue(usernameInput.name, `@${formik.values.username}`);
  }
  return formik.handleBlur(usernameInput.name);
};
