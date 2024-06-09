import { MainBoxStateEnum } from "@/components/Chat/chat.types";
import { configList } from "@/components/Chat/InteractiveList/variants/Settings/settings.config";
import { CustomButton } from "@/components/CustomButton/CustomButton";
import { ScrollList } from "@/components/ScrollList/ScrollList";
import { setCurrentMainBoxState } from "@/lib/features/chat/chatSlice";
import { useAppDispatch } from "@/lib/hooks";

export function Settings() {
  const dispatch = useAppDispatch();

  const handlePressProfile = () => {
    dispatch(setCurrentMainBoxState(MainBoxStateEnum.PROFILE_SETTINGS));
  };
  return (
    <ScrollList>
      {configList.map(title => {
        return (
          <CustomButton
            onClick={handlePressProfile}
            variant="square"
            className="text-lg text-start justify-start dark:bg-dark-300 dark:hover:bg-dark-200"
            key={title}
          >
            {title}
          </CustomButton>
        );
      })}
    </ScrollList>
  );
}
