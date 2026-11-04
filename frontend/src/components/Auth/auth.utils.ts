import { AxiosError } from "axios";
import { FormikValues } from "formik";
import { toast } from "react-toastify";

import { messages, usernameInput } from "@/components/Auth/auth.config";
import { formatSystemAlertMessage } from "@/helpers/formatSystemAlertMessage";

export const handleUsernameBlur = (formik: FormikValues) => {
  if (!formik.values.username.startsWith("@")) {
    formik.setFieldValue(usernameInput.name, `@${formik.values.username}`);
  }
  return formik.handleBlur(usernameInput.name);
};

export const axiosErrorHandler = (error: unknown) => {
  console.error("Auth request failed:", error);
  if (error instanceof AxiosError) {
    if (!error.response) {
      // Không có phản hồi: thường do gateway chưa chạy, sai URL, hoặc bị chặn CORS
      toast.error("Không kết nối được máy chủ. Kiểm tra API gateway (8080), Eureka và CORS.");
    } else {
      const message = formatSystemAlertMessage(error.response.data?.message);
      toast.error(message ?? messages.DEFAULT_ERROR_MESSAGE);
    }
  } else {
    toast.error(messages.DEFAULT_ERROR_MESSAGE);
  }
  toast.clearWaitingQueue();
};
