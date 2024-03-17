import React from "react";

import { StatusProps } from "@/components/Status/status.types";

export function Status({ className, ...props }: StatusProps) {
  return <div className={`bg-green rounded-full w-2.5 h-2.5 mr-1 ${className}`} {...props} />;
}
