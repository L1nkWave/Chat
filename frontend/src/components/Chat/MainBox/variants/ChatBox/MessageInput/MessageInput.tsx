import { CustomButton } from "@/components/CustomButton/CustomButton";
import { CustomInput } from "@/components/CustomInput/CustomInput";
import { Icon } from "@/components/Icon/Icon";
import { COLORS } from "@/constants/colors";

export function MessageInput() {
  return (
    <div className="z-20 absolute w-full bottom-0 bg-transparent flex justify-between px-16 pb-10">
      <div className="dark:bg-dark-200 rounded-xl flex flex-row w-3/4 items-center justify-center">
        <Icon name="folder-outline" className="ml-4" color={COLORS.blue["200"]} />
        <CustomInput
          containerClassName="w-full"
          className="p-2 dark:placeholder-blue-300 dark:text-gray-200"
          innerContainerClassName="bg-transparent rounded-2xl text-base font-normal !border-0"
          placeholder="Type message..."
        />
      </div>
      <CustomButton variant="square" className="w-1/6 py-2 !bg-blue-100 !dark:bg-blue-100">
        <Icon name="send-outline" iconSize={38} color={COLORS.white} />
      </CustomButton>
      <div className="-z-10 absolute inset-x-0 bottom-0 h-full bg-dark-550 blur opacity-98 rounded-xl" />
      <div className="-z-10 absolute inset-x-0 bottom-0 h-5/6 bg-dark-550" />
    </div>
  );
}
