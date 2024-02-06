import "./systemToastContainer.styles.css";

import React from "react";
import { ToastContainer, ToastContainerProps } from "react-toastify";

import { systemToastContainerConfig } from "@/components/SystemToastContainer/systemToastContainer.config";

export function SystemToastContainer(props: Readonly<ToastContainerProps>) {
  return <ToastContainer {...props} {...systemToastContainerConfig} />;
}
