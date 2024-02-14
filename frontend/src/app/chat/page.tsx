import React from "react";

import { Chat } from "@/components/Chat/Chat";
import { Container } from "@/components/Container/Container";
import { SocketProvider } from "@/context/SocketProvider/SocketProvider";

export default function ChatPage() {
  return (
    <Container showHeader={false}>
      <SocketProvider>
        <Chat />
      </SocketProvider>
    </Container>
  );
}
