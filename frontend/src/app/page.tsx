import React from "react";

import { tutorialCards } from "@/app/tutorialCards";
import { Header } from "@/components/Header/Header";
import { HomeContainer } from "@/components/HomeContainer/HomeContainer";
import { HomePageParallax } from "@/components/HomePageParallax/HomePageParallax";
import { TutorialCard } from "@/components/TutorialCard/TutorialCard";

export default function Home() {
  return (
    <>
      <Header />
      <HomePageParallax />
      <HomeContainer>
        {tutorialCards.map((card, index) => (
          <TutorialCard key={card.text} index={index} {...card} />
        ))}
      </HomeContainer>
    </>
  );
}
