import { CustomButton } from "@/components/CustomButton/CustomButton";
import { Icon, IconName } from "@/components/Icon/Icon";

export type GroupDetailsButtonProps = {
  title: string;
  description: string;
  iconName: IconName;
  className?: string;
  onClick?: () => void;
};

export function GroupDetailsButton({
  onClick: handleClick,
  iconName,
  title,
  description,
  className,
}: Readonly<GroupDetailsButtonProps>) {
  return (
    <CustomButton
      onClick={handleClick}
      variant="transparent"
      className={`w-full rounded-none p-4 justify-between flex items-center hover:bg-dark-200 ${className ?? className}`}
    >
      <span className="text-blue-200 text-xl flex gap-4 font-semibold pr-8">
        <Icon name={iconName} iconSize={32} />
        {title}
      </span>
      <span className="text-base text-gray-500">{description}</span>
    </CustomButton>
  );
}
