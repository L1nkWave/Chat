import Image from "next/image";

type LogoProps = {
    size?: number;
};

/** Official SGU logo, used as-is: no recolouring, no stretching. */
export function Logo({ size = 40 }: LogoProps) {
    return (
        <Image
            src="/brand/sgu-logo.png"
            alt="Logo Đại học Sài Gòn"
            width={size}
            height={size}
            priority
        />
    );
}