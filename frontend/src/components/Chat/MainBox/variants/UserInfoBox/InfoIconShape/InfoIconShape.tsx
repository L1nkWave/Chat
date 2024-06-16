import { InfoIconShapeProps } from "@/components/Chat/MainBox/variants/UserInfoBox/InfoIconShape/infoIconShape.types";
import { Icon } from "@/components/Icon/Icon";

export function InfoIconShape({ icon, iconSize = 20 }: Readonly<InfoIconShapeProps>) {
  return (
    <div className="flex mr-4 bg-dark-150 rounded-xl w-min-8 p-2 justify-center items-center">
      <Icon name={icon} className="text-blue-200" iconSize={iconSize} />
    </div>
  );
}
