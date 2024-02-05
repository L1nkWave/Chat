import { Slide, ToastContainerProps } from "react-toastify";

export const systemToastContainerConfig: ToastContainerProps = {
  position: "top-center",
  autoClose: 5000,
  limit: 1,
  hideProgressBar: false,
  newestOnTop: false,
  closeOnClick: true,
  rtl: false,
  pauseOnFocusLoss: true,
  draggable: true,
  pauseOnHover: true,
  transition: Slide,
};
