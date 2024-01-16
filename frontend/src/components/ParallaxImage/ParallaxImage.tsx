import Image, { ImageProps } from "next/image";
import { ForwardedRef, forwardRef } from "react";

export const ParallaxImage = forwardRef(
  (props: ImageProps, ref: ForwardedRef<HTMLImageElement>) => {
    return <Image width={500} height={500} ref={ref} {...props} />;
  }
);
