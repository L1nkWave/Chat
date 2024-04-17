import { PropsWithChildren } from "react";

export function InfoTextBox({ children }: Readonly<PropsWithChildren>) {
  return <div className="p-2 flex text-gray-400 text-xl">{children}</div>;
}
