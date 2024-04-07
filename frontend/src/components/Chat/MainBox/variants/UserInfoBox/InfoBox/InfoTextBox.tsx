import { PropsWithChildren } from "react";

export function InfoTextBox({ children }: Readonly<PropsWithChildren>) {
  return <p className="p-2 flex text-gray-400 text-xl">{children}</p>;
}
