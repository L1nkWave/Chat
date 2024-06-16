import Image from "next/image";
import React from "react";

export type PresentCardProps = {
  text: string;
  title: string;
  imageSrc: string;
  index: number;
  width: number;
  height: number;
};
export function TutorialCard({ width, height, text, imageSrc, title, index }: PresentCardProps) {
  const isIndexEven = index % 2 === 0;
  return (
    <div className={`flex items-center ${isIndexEven && "justify-end"}`}>
      {!isIndexEven && (
        <Image
          width={width}
          height={height}
          src={imageSrc}
          style={{ height: "auto", width: "auto" }}
          quality={100}
          alt="Tutorial image"
          className="mr-10 rounded-2xl self-end"
        />
      )}
      <span className="w-[41%] text-start">
        <div className="text-6xl font-bold text-center">{title}</div>
        <p className="mt-10 text-2xl">{text}</p>
      </span>
      {isIndexEven && (
        <Image
          width={width}
          height={height}
          style={{ height: "auto", width: "auto" }}
          src={imageSrc}
          quality={100}
          alt="Tutorial image"
          className="ml-10 rounded-2xl self-start"
        />
      )}
    </div>
  );
}
