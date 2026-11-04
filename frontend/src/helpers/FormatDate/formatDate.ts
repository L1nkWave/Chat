/**
 * Chuyển giá trị thời gian từ backend thành Date.
 * - Số: epoch tính bằng giây (kiểu cũ).
 * - Chuỗi số: epoch tính bằng giây.
 * - Chuỗi ISO (ví dụ "2026-11-04T08:56:52.615Z"): backend Jackson 3 trả về dạng này.
 */
import { format } from "date-fns";

import { DateFormat } from "@/helpers/FormatDate/formatDate.types";

export const formatDate = (date: number | Date, dateFormat: DateFormat): string => {
  return format(date, dateFormat);
};

export const toDate = (value: number | string): Date => {
  if (typeof value === "number") {
    return new Date(value * 1000);
  }

  if (/^\d+(\.\d+)?$/.test(value)) {
    return new Date(Number(value) * 1000);
  }

  return new Date(value);
};
