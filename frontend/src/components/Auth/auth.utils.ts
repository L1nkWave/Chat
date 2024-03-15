import { AxiosError } from "axios";
import { FormikValues } from "formik";
import { toast } from "react-toastify";

import { messages, usernameInput } from "@/components/Auth/auth.config";
import { formatMessage } from "@/helpers/formatMessage";

export const handleUsernameBlur = (formik: FormikValues) => {
  if (!formik.values.username.startsWith("@")) {
    formik.setFieldValue(usernameInput.name, `@${formik.values.username}`);
  }
  return formik.handleBlur(usernameInput.name);
};

export const axiosErrorHandler = (error: unknown) => {
  if (error instanceof AxiosError) {
    toast.error(formatMessage(error.response?.data.message) ?? messages.DEFAULT_ERROR_MESSAGE);
  } else {
    toast.error(messages.DEFAULT_ERROR_MESSAGE);
  }
  toast.clearWaitingQueue();
};
