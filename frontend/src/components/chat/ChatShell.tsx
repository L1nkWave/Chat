"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Logo } from "@/components/brand/Logo";
import { ThemeToggle } from "@/components/theme/ThemeToggle";
import { apiFetch } from "@/lib/api/client";
import { logout } from "@/lib/auth/auth-api";
import { setAccessToken } from "@/lib/auth/session";
import { chatApi, errorMessage } from "@/lib/chat/api";
import { uploadFile } from "@/lib/chat/files";
import type { Conversation, Invitation, Member, Message } from "@/lib/chat/types";
import { ChatSocket, type ConnectionStatus } from "@/lib/ws/chat-socket";
import type { ChatEvent } from "@/lib/ws/protocol";
import { Composer } from "./Composer";
import { CreateGroupForm, InvitationList, MembersPanel } from "./GroupPanels";
import { MessageList } from "./MessageList";

type TokensDto = { accessToken: string; refreshExpiration: number };
type Notice = { text: string; tone: "error" | "info" };

const STATUS_LABEL: Record<ConnectionStatus, string> = {
    connecting: "Đang kết nối...",
    open: "Đã kết nối",
    reconnecting: "Mất kết nối, đang thử lại...",
    closed: "Đã ngắt kết nối",
};

function previewOf(message: Message): string {
    if (message.text) return message.text;
    return message.attachment ? `📎 ${message.attachment.filename}` : "";
}

export function ChatShell() {
    const router = useRouter();
    const [ready, setReady] = useState(false);
    const [me, setMe] = useState("");
    const [connection, setConnection] = useState<ConnectionStatus>("connecting");
    const [notice, setNotice] = useState<Notice | null>(null);
    const [conversations, setConversations] = useState<Conversation[]>([]);
    const [invitations, setInvitations] = useState<Invitation[]>([]);
    const [messages, setMessages] = useState<Record<string, Message[]>>({});
    const [membersState, setMembersState] = useState<{
        groupId: string;
        list: Member[];
    } | null>(null);
    const [membersVersion, setMembersVersion] = useState(0);
    const [showMembers, setShowMembers] = useState(false);
    const [showCreate, setShowCreate] = useState(false);
    const [activeId, setActiveId] = useState<string | null>(null);
    const [query, setQuery] = useState("");

    const socketRef = useRef<ChatSocket | null>(null);
    const activeIdRef = useRef<string | null>(null);
    const meRef = useRef("");
    const disconnectedRef = useRef(false);

    useEffect(() => {
        activeIdRef.current = activeId;
    }, [activeId]);

    useEffect(() => {
        meRef.current = me;
    }, [me]);

    const showNotice = useCallback((text: string, tone: Notice["tone"]) => {
        setNotice({ text, tone });
    }, []);

    const loadConversations = useCallback(async () => {
        try {
            setConversations(await chatApi.listConversations());
        } catch (err) {
            showNotice(errorMessage(err), "error");
        }
    }, [showNotice]);

    const loadInvitations = useCallback(async () => {
        try {
            setInvitations(await chatApi.listInvitations());
        } catch (err) {
            showNotice(errorMessage(err), "error");
        }
    }, [showNotice]);

    const loadMessages = useCallback(
        async (conversationId: string) => {
            try {
                const list = await chatApi.listMessages(conversationId);
                setMessages((prev) => ({ ...prev, [conversationId]: list }));
            } catch (err) {
                showNotice(errorMessage(err), "error");
            }
        },
        [showNotice],
    );

    // Sau khi kết nối lại, tải lại toàn bộ trạng thái để bù các tin và sự kiện đã bỏ lỡ.
    const resync = useCallback(() => {
        void loadConversations();
        void loadInvitations();
        const id = activeIdRef.current;
        if (id) void loadMessages(id);
    }, [loadConversations, loadInvitations, loadMessages]);

    // Khôi phục phiên từ refresh cookie. Access token chỉ nằm trong bộ nhớ,
    // nên sau khi tải lại trang phải xin token mới.
    useEffect(() => {
        let cancelled = false;
        apiFetch<TokensDto>("/api/v1/auth/refresh-tokens", {
            method: "POST",
            skipAuth: true,
            skipRefresh: true,
        })
            .then((tokens) => {
                if (cancelled) return;
                setAccessToken(tokens.accessToken);
                setReady(true);
            })
            .catch(() => {
                if (!cancelled) router.replace("/login");
            });
        return () => {
            cancelled = true;
        };
    }, [router]);

    // Tải dữ liệu ban đầu sau khi có phiên.
    useEffect(() => {
        if (!ready) return;
        void loadConversations();
        void loadInvitations();
    }, [ready, loadConversations, loadInvitations]);

    // Mở WebSocket và xử lý sự kiện từ server.
    useEffect(() => {
        if (!ready) return;

        const receive = (message: Message) => {
            setMessages((prev) => {
                const list = prev[message.conversationId] ?? [];
                // Bỏ qua tin trùng, vì tin có thể vừa được tải lại khi kết nối lại.
                if (list.some((m) => m.id === message.id)) return prev;
                return { ...prev, [message.conversationId]: [...list, message] };
            });
            setConversations((prev) => {
                const target = prev.find((c) => c.id === message.conversationId);
                if (!target) return prev;
                const isOpen = activeIdRef.current === message.conversationId;
                const isOwn = message.senderId === meRef.current;
                const unread = isOpen || isOwn ? 0 : target.unread + 1;
                return [
                    {
                        ...target,
                        lastMessage: previewOf(message),
                        lastTime: message.time,
                        unread,
                    },
                    ...prev.filter((c) => c.id !== message.conversationId),
                ];
            });
        };

        const socket = new ChatSocket({
            onSession: ({ userId }) => setMe(userId),
            onChatEvent: (event: ChatEvent) => {
                if (event.type === "message") {
                    receive(event.message);
                } else if (event.type === "invitation") {
                    void loadInvitations();
                } else if (event.type === "membership") {
                    void loadConversations();
                    setMembersVersion((v) => v + 1);
                }
            },
            onError: (error) => showNotice(error.message, "error"),
            onStatus: (status) => {
                setConnection(status);
                if (status === "reconnecting") disconnectedRef.current = true;
                if (status === "open" && disconnectedRef.current) {
                    disconnectedRef.current = false;
                    resync();
                }
            },
            onAuthFailed: () => router.replace("/login"),
        });
        socketRef.current = socket;
        socket.start();

        return () => {
            socket.stop();
            socketRef.current = null;
        };
    }, [ready, router, loadConversations, loadInvitations, showNotice, resync]);

    // Tải tin nhắn của cuộc trò chuyện đang mở.
    useEffect(() => {
        if (activeId) void loadMessages(activeId);
    }, [activeId, loadMessages]);

    // Tải danh sách thành viên khi bảng thành viên đang mở.
    useEffect(() => {
        if (!activeId || !showMembers) return;
        let cancelled = false;
        chatApi
            .listMembers(activeId)
            .then((list) => {
                if (!cancelled) setMembersState({ groupId: activeId, list });
            })
            .catch((err) => {
                if (!cancelled) showNotice(errorMessage(err), "error");
            });
        return () => {
            cancelled = true;
        };
    }, [activeId, showMembers, membersVersion, showNotice]);

    const active = conversations.find((c) => c.id === activeId) ?? null;
    const activeMessages = activeId ? (messages[activeId] ?? []) : [];
    const activeMembers =
        membersState && membersState.groupId === activeId ? membersState.list : null;
    const isOwner = activeMembers?.some((m) => m.userId === me && m.isOwner) ?? false;

    const visible = useMemo(() => {
        const q = query.trim().toLowerCase();
        return conversations.filter((c) => c.title.toLowerCase().includes(q));
    }, [conversations, query]);

    function openConversation(id: string) {
        setNotice(null);
        setShowMembers(false);
        setShowCreate(false);
        setActiveId(id);
        setConversations((prev) =>
            prev.map((c) => (c.id === id ? { ...c, unread: 0 } : c)),
        );
    }

    function handleCreated(groupId: string) {
        setShowCreate(false);
        void loadConversations();
        openConversation(groupId);
    }

    function handleLeft() {
        setShowMembers(false);
        setActiveId(null);
        void loadConversations();
    }

    // Tin nhắn chỉ hiện khi server phát lại. Tệp được tải lên trước,
    // rồi gửi kèm attachmentId qua WebSocket.
    async function sendMessage(text: string, file: File | null): Promise<boolean> {
        if (!activeId) return false;
        const socket = socketRef.current;
        if (!socket?.isOpen()) {
            showNotice("Chưa kết nối được tới máy chủ. Tin nhắn chưa được gửi.", "error");
            return false;
        }

        let attachmentId: string | undefined;
        if (file) {
            try {
                const attachment = await uploadFile(file);
                attachmentId = attachment.id;
            } catch (err) {
                showNotice(errorMessage(err), "error");
                return false;
            }
        }

        const sent = socket.send(activeId, text, attachmentId);
        if (!sent) showNotice("Mất kết nối trước khi gửi. Vui lòng thử lại.", "error");
        return sent;
    }

    async function handleLogout() {
        try {
            await logout();
        } finally {
            router.replace("/login");
        }
    }

    if (!ready) {
        return (
            <div className="grid min-h-dvh place-items-center bg-paper text-sm text-muted">
                Đang tải...
            </div>
        );
    }

    return (
        <div className="flex h-dvh flex-col bg-paper text-ink">
            <header className="flex items-center justify-between gap-3 border-b border-line bg-surface px-4 py-3">
                <div className="flex items-center gap-3">
                    <Logo />
                    <span className="font-serif text-lg font-semibold">SGU Chat</span>
                </div>
                <div className="flex items-center gap-3">
          <span
              className={`hidden text-xs sm:inline ${
                  connection === "open" ? "text-success" : "text-muted"
              }`}
              role="status"
          >
            {STATUS_LABEL[connection]}
          </span>
                    <ThemeToggle />
                    <button
                        type="button"
                        onClick={handleLogout}
                        className="rounded-lg border border-line px-3 py-2 text-sm transition-colors hover:bg-paper"
                    >
                        Đăng xuất
                    </button>
                </div>
            </header>

            {notice ? (
                <div
                    role="status"
                    className={`border-b border-line bg-surface px-4 py-2 text-sm ${
                        notice.tone === "error" ? "text-danger" : "text-primary"
                    }`}
                >
                    {notice.text}
                </div>
            ) : null}

            <div className="flex min-h-0 flex-1">
                <aside
                    className={`${active ? "hidden md:flex" : "flex"} w-full flex-col border-r border-line bg-surface md:w-80`}
                >
                    <div className="space-y-2 p-3">
                        <input
                            type="search"
                            value={query}
                            onChange={(e) => setQuery(e.target.value)}
                            placeholder="Tìm cuộc trò chuyện"
                            aria-label="Tìm cuộc trò chuyện"
                            className="h-10 w-full rounded-lg border border-line bg-paper px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-sky"
                        />
                        <button
                            type="button"
                            onClick={() => setShowCreate((v) => !v)}
                            aria-expanded={showCreate}
                            className="h-9 w-full rounded-lg border border-line text-sm transition-colors hover:bg-paper"
                        >
                            {showCreate ? "Đóng" : "+ Tạo nhóm mới"}
                        </button>
                    </div>

                    {showCreate ? (
                        <CreateGroupForm
                            onCreated={handleCreated}
                            onCancel={() => setShowCreate(false)}
                            onError={(text) => showNotice(text, "error")}
                        />
                    ) : null}

                    {invitations.length > 0 ? (
                        <InvitationList
                            invitations={invitations}
                            onChanged={() => {
                                void loadInvitations();
                                void loadConversations();
                            }}
                            onError={(text) => showNotice(text, "error")}
                        />
                    ) : null}

                    <ul className="min-h-0 flex-1 overflow-y-auto">
                        {visible.map((c) => {
                            const isActive = c.id === activeId;
                            return (
                                <li key={c.id}>
                                    <button
                                        type="button"
                                        onClick={() => openConversation(c.id)}
                                        aria-current={isActive ? "true" : undefined}
                                        className={`flex w-full items-start gap-3 px-4 py-3 text-left transition-colors hover:bg-paper ${
                                            isActive ? "bg-paper" : ""
                                        }`}
                                    >
                    <span className="grid size-10 shrink-0 place-items-center rounded-full bg-primary text-sm font-semibold text-primary-ink">
                      {c.title.charAt(0)}
                    </span>
                                        <span className="min-w-0 flex-1">
                      <span className="flex items-center justify-between gap-2">
                        <span className="truncate font-medium">{c.title}</span>
                        <span className="shrink-0 text-xs text-muted">{c.lastTime}</span>
                      </span>
                      <span className="flex items-center justify-between gap-2">
                        <span className="truncate text-sm text-muted">{c.lastMessage}</span>
                          {c.unread > 0 ? (
                              <span className="shrink-0 rounded-full bg-primary px-2 text-xs text-primary-ink">
                            {c.unread}
                          </span>
                          ) : null}
                      </span>
                    </span>
                                    </button>
                                </li>
                            );
                        })}
                    </ul>
                </aside>

                <main className={`${active ? "flex" : "hidden md:flex"} min-w-0 flex-1 flex-col`}>
                    {active ? (
                        <>
                            <div className="flex items-center justify-between gap-3 border-b border-line bg-surface px-4 py-3">
                                <div className="flex min-w-0 items-center gap-3">
                                    <button
                                        type="button"
                                        onClick={() => setActiveId(null)}
                                        className="shrink-0 rounded-lg px-2 py-1 text-sm text-primary md:hidden"
                                    >
                                        ← Danh sách
                                    </button>
                                    <div className="min-w-0">
                                        <h2 className="truncate font-serif text-lg font-semibold">{active.title}</h2>
                                        <p className="text-xs text-muted">
                                            {active.kind === "group" ? "Nhóm" : "Trò chuyện trực tiếp"}
                                        </p>
                                    </div>
                                </div>
                                {active.kind === "group" ? (
                                    <button
                                        type="button"
                                        aria-pressed={showMembers}
                                        onClick={() => setShowMembers((v) => !v)}
                                        className="shrink-0 rounded-lg border border-line px-3 py-1.5 text-sm transition-colors hover:bg-paper"
                                    >
                                        Thành viên
                                    </button>
                                ) : null}
                            </div>

                            {showMembers && active.kind === "group" ? (
                                activeMembers ? (
                                    <MembersPanel
                                        groupId={active.id}
                                        members={activeMembers}
                                        isOwner={isOwner}
                                        onLeft={handleLeft}
                                        onChanged={() => setMembersVersion((v) => v + 1)}
                                        onNotice={showNotice}
                                    />
                                ) : (
                                    <div className="border-b border-line bg-surface px-4 py-3 text-sm text-muted">
                                        Đang tải thành viên...
                                    </div>
                                )
                            ) : null}

                            <MessageList
                                messages={activeMessages}
                                currentUserId={me}
                                isGroup={active.kind === "group"}
                            />
                            <Composer onSend={sendMessage} />
                        </>
                    ) : (
                        <div className="m-auto px-6 text-center text-sm text-muted">
                            Chọn một cuộc trò chuyện để bắt đầu, hoặc tạo nhóm mới.
                        </div>
                    )}
                </main>
            </div>
        </div>
    );
}