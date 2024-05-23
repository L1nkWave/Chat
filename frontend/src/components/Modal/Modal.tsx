import React, { PropsWithChildren } from "react";

import { CustomButton } from "@/components/CustomButton/CustomButton";
import { Icon } from "@/components/Icon/Icon";
import { COLORS } from "@/constants/colors";

export type ModalProps = {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: () => void;
  confirmButtonTitle: string;
  submitButtonColor?: string;
};

export function Modal({
  isOpen,
  children,
  confirmButtonTitle,
  submitButtonColor,
  onClose: handleClose,
  onSubmit: handleSubmit,
}: PropsWithChildren<ModalProps>) {
  if (!isOpen) return null;

  return (
    <div className="relative z-50" aria-labelledby="modal-title" role="dialog" aria-modal="true">
      <div className="fixed inset-0 bg-dark-500 bg-opacity-75 transition-opacity" />
      <div className="fixed inset-0 z-10 w-screen overflow-y-auto">
        <div className="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
          <div className="relative transform overflow-hidden rounded-2xl border dark:border-dark-150 dark:bg-dark-550 text-left shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg">
            {children}
            <div className="dark:bg-dark-550 mb-10 px-4 py-3 sm:flex sm:flex-row sm:px-6 gap-12 items-center justify-center">
              <CustomButton variant="outline" onClick={handleClose} className="px-1 py-1">
                <Icon name="left-angle" iconSize={28} color={COLORS.blue["200"]} />
              </CustomButton>
              <CustomButton
                onClick={handleSubmit}
                type="button"
                className={`font-bold dark:bg-blue-100 text-lg px-8 ${submitButtonColor ?? submitButtonColor}`}
                variant="flattened"
              >
                {confirmButtonTitle}
              </CustomButton>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
