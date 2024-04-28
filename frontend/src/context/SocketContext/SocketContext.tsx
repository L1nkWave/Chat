import { createContext } from "react";

import { SocketContextProps } from "@/context/SocketContext/socketContext.types";

const SocketContext = createContext<SocketContextProps>({});

export { SocketContext };
