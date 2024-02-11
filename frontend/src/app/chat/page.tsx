import React from "react";

import { Container } from "@/components/Container/Container";
import { SocketProvider } from "@/context/SocketProvider/SocketProvider";

export default function ChatPage() {
  return (
    <Container showHeader={false}>
      <SocketProvider>Chat</SocketProvider>
    </Container>
  );
}
