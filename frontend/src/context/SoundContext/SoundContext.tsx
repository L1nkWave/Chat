import { createContext } from "react";

export type SoundContextProps = {
  messageSound: () => void;
};

const messageSound = () => {
  new Audio("/sound/message.mp3").play();
};

const SoundContext = createContext<SoundContextProps>({ messageSound });

export { SoundContext };
