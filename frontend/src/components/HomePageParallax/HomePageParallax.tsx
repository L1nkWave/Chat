"use client";

import "./homePageParallax.css";

import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/dist/ScrollTrigger";
import { MutableRefObject, useLayoutEffect, useRef } from "react";

import { CustomButton } from "@/components/CustomButton/CustomButton";
import { ParallaxImage } from "@/components/ParallaxImage/ParallaxImage";

export function HomePageParallax() {
  const parallaxRef = useRef(null);
  const elements = {
    stroke: useRef(null),
    frontWave: useRef(null),
    middleWave: useRef(null),
    backWave: useRef(null),
    link: useRef(null),
    centerText: useRef(null),
    joinButton: useRef(null),
  };

  useLayoutEffect(() => {
    const context = gsap.context(() => {
      gsap.registerPlugin(ScrollTrigger);
      const timeline = gsap.timeline({
        defaults: { duration: 1 },
        scrollTrigger: {
          trigger: parallaxRef.current,
          start: "top top",
          end: "5000 bottom",
          scrub: true,
          pin: true,
        },
      });
      const addToTimeline = (
        target: MutableRefObject<null>,
        props: gsap.TweenVars,
        position: number
      ) => {
        timeline.to(target.current, props, position);
      };

      addToTimeline(elements.frontWave, { x: "-=75%" }, 0);
      addToTimeline(elements.middleWave, { x: "-=85%" }, 0);
      addToTimeline(elements.stroke, { opacity: 0 }, -0.1);
      addToTimeline(elements.stroke, { x: "+=64%" }, 0);
      addToTimeline(elements.backWave, { x: "+=65%" }, 0);
      addToTimeline(elements.link, { y: "+=100%", opacity: 0 }, 0);
      addToTimeline(elements.centerText, { y: "-=150%", opacity: 1 }, 0);
      addToTimeline(elements.joinButton, { opacity: 1 }, 1);
    });
    return () => context.revert();
  }, [
    elements.backWave,
    elements.centerText,
    elements.frontWave,
    elements.joinButton,
    elements.link,
    elements.middleWave,
    elements.stroke,
  ]);

  return (
    <div className="overflow-hidden">
      <div ref={parallaxRef} className="parallax">
        <ParallaxImage
          ref={elements.stroke}
          src="/images/homePageParallax/stroke.svg"
          className="stroke"
          alt="stroke"
        />
        <ParallaxImage
          ref={elements.frontWave}
          alt="front-wave"
          className="front-wave"
          src="/images/homePageParallax/front-wave.svg"
        />
        <ParallaxImage
          ref={elements.middleWave}
          priority
          src="/images/homePageParallax/middle-wave.svg"
          className="middle-wave"
          alt="middle-wave"
        />
        <ParallaxImage
          ref={elements.backWave}
          className="back-wave"
          src="/images/homePageParallax/back-wave.svg"
          alt="back-wave"
        />
        <ParallaxImage
          ref={elements.link}
          src="/images/homePageParallax/link.svg"
          className="link"
          alt="link"
        />

        <div ref={elements.centerText} className="parallax-text gap-4 z-10">
          <h1 className="font-bold dark:text-gray-200 text-8xl">
            Link Wave Chat
          </h1>
          <CustomButton className="opacity-0" ref={elements.joinButton}>
            Join
          </CustomButton>
        </div>
      </div>
    </div>
  );
}
