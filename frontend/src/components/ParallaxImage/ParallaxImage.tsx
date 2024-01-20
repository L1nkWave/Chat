import Image, { ImageProps } from "next/image";
import { ForwardedRef, forwardRef } from "react";

import {
  PARALLAX_IMAGE_HEIGHT,
  PARALLAX_IMAGE_WIDTH,
} from "@/components/ParallaxImage/parallaxImage.config";

export const ParallaxImage = forwardRef(
  (props: ImageProps, ref: ForwardedRef<HTMLImageElement>) => {
    return (
      <Image
        width={PARALLAX_IMAGE_WIDTH}
        height={PARALLAX_IMAGE_HEIGHT}
        ref={ref}
        {...props}
      />
    );
  }
);
