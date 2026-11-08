"use client";

import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { Logo } from "@/components/brand/Logo";
import { ThemeToggle } from "@/components/theme/ThemeToggle";
import { apiFetch } from "@/lib/api/client";
import { logout } from "@/lib/auth/auth-api";
import { setAccessToken } from "@/lib/auth/session";
import { chatApi, errorMessage } from "@/lib/chat/api";
import { uploadFile, type CreatedFile } from "@/lib/chat/files";
import { formatChatTime } from "@/lib/chat/time";
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

const ICONS = {
    chat: "M21 12a8 8 0 01-11.6 7.1L4 20l1-4.6A8 8 0 1121 12z",
    search: "M11 19a8 8 0 100-16 8 8 0 000 16zM21 21l-4.3-4.3",
    logout: "M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4M16 17l5-5-5-5M21 12H9",
    users: "M16 21v-2a4 4 0 00-4-4H6a4 4 0 00-4 4v2M9 11a4 4 0 100-8 4 4 0 000 8zM22 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75",
    plus: "M12 5v14M5 12h14",
    back: "M15 18l-6-6 6-6",
};

function Icon({ d, size = 20 }: { d: string; size?: number }) {
    return (
        <svg
            viewBox="0 0 24 24"
            width={size}
            height={size}
            fill="none"
            stroke="currentColor"
            strokeWidth={1.8}
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
        >
            <path d={d} />
        </svg>
    );
}

function Avatar({ title, group, size = "md" }: { title: string; group: boolean; size?: "sm" | "md" }) {
    const dims = size === "md" ? "size-11 text-base" : "size-9 text-sm";
    return (
        <span
            aria-hidden="true"
            className={`${dims} grid shrink-0 place-items-center rounded-md font-semibold ${
                group ? "bg-primary text-primary-ink" : "bg-tint text-primary"
            }`}
        >
            {title.trim().charAt(0).toUpperCase() || "?"}
        </span>
    );
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
    const [showInfo, setShowInfo] = useState(true);
    const [showCreate, setShowCreate] = useState(false);
    const [activeId, setActiveId] = useState<string | null>(null);
    const [query, setQuery] = useState("");
    const [profile, setProfile] = useState<{ name: string; username: string } | null>(null);
    const [showDirect, setShowDirect] = useState(false);
    const [directName, setDirectName] = useState("");
    const [directBusy, setDirectBusy] = useState(false);

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

    // Thông báo hiện như toast rồi tự tắt sau vài giây
    useEffect(() => {
        if (!notice) return;
        const timer = window.setTimeout(() => setNotice(null), notice.tone === "error" ? 5000 : 3500);
        return () => window.clearTimeout(timer);
    }, [notice]);

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
                    if (event.kicked) {
                        // Bị xóa khỏi nhóm: bỏ lịch sử đã cache và đóng khung chat ngay
                        setMessages((prev) => {
                            const next = { ...prev };
                            delete next[event.conversationId];
                            return next;
                        });
                        setActiveId((current) => (current === event.conversationId ? null : current));
                        setShowMembers(false);
                        showNotice("Bạn đã bị xóa khỏi nhóm.", "error");
                    }
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

    // Hồ sơ người đang đăng nhập, hiển thị ở đầu trang
    useEffect(() => {
        if (!me) return;
        apiFetch<{ name?: string; username?: string }>(`/api/v1/users/${me}`)
            .then((u) => setProfile({ name: u.name ?? "", username: u.username ?? "" }))
            .catch(() => setProfile(null));
    }, [me]);

    // Tải tin nhắn của cuộc trò chuyện đang mở.
    useEffect(() => {
        if (activeId) void loadMessages(activeId);
    }, [activeId, loadMessages]);

    // Tải danh sách thành viên khi bảng thành viên đang mở.
    useEffect(() => {
        const isGroupOpen = conversations.some((c) => c.id === activeId && c.kind === "group");
        if (!activeId || !isGroupOpen) return;
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
    }, [activeId, conversations, membersVersion, showNotice]);

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

    // Tin nhắn chỉ hiện khi server phát lại. Tệp được tải lên qua REST trước,
    // rồi báo server qua WebSocket /send_file để phát tới người nhận.
    async function sendMessage(text: string, file: File | null): Promise<boolean> {
        if (!activeId) return false;
        const socket = socketRef.current;
        if (!socket?.isOpen()) {
            showNotice("Chưa kết nối được tới máy chủ. Tin nhắn chưa được gửi.", "error");
            return false;
        }

        if (file) {
            let created: CreatedFile;
            try {
                created = await uploadFile(activeId, file);
            } catch (err) {
                showNotice(errorMessage(err), "error");
                return false;
            }
            if (!socket.sendFile(activeId, created)) {
                showNotice("Mất kết nối trước khi gửi. Vui lòng thử lại.", "error");
                return false;
            }
        }

        if (text.trim()) {
            if (!socket.send(activeId, text)) {
                showNotice("Mất kết nối trước khi gửi. Vui lòng thử lại.", "error");
                return false;
            }
        }
        return true;
    }

    async function startDirect(e: FormEvent<HTMLFormElement>) {
        e.preventDefault();
        const username = directName.trim();
        if (!username) return;
        setDirectBusy(true);
        try {
            const conversation = await chatApi.startDirectChat(username);
            setDirectName("");
            setShowDirect(false);
            await loadConversations();
            openConversation(conversation.id);
        } catch (err) {
            showNotice(errorMessage(err), "error");
        } finally {
            setDirectBusy(false);
        }
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
        <div className="flex h-dvh bg-paper text-ink">
            <nav
                aria-label="Điều hướng"
                className="hidden w-16 shrink-0 flex-col items-center justify-between border-r border-line bg-surface py-4 md:flex"
            >
                <Logo size={36} />
                <div className="flex flex-col items-center gap-3">
                    <span
                        title="Tin nhắn"
                        className="grid size-10 place-items-center rounded-md bg-tint text-primary"
                    >
                        <Icon d={ICONS.chat} />
                    </span>
                    <ThemeToggle />
                </div>
                <button
                    type="button"
                    onClick={handleLogout}
                    aria-label="Đăng xuất"
                    title="Đăng xuất"
                    className="grid size-10 place-items-center rounded-md text-muted transition-colors hover:bg-tint hover:text-ink"
                >
                    <Icon d={ICONS.logout} />
                </button>
            </nav>

            <aside
                className={`${active ? "hidden md:flex" : "flex"} min-h-0 w-full flex-col border-r border-line bg-surface md:w-[340px]`}
            >
                <header className="flex items-start justify-between gap-3 px-5 pb-3 pt-5">
                    <div className="min-w-0">
                        <h1 className="font-serif text-2xl font-semibold leading-tight">Tin nhắn</h1>
                        {profile ? (
                            <p className="truncate text-xs text-muted">
                                {profile.name} · @{profile.username}
                            </p>
                        ) : null}
                    </div>
                    <div className="flex items-center gap-3">
                        <span
                            role="status"
                            className={`flex items-center gap-1.5 text-xs ${
                                connection === "open" ? "text-success" : "text-muted"
                            }`}
                        >
                            <span
                                aria-hidden="true"
                                className={`size-2 rounded-full ${connection === "open" ? "bg-success" : "bg-muted"}`}
                            />
                            <span className="sr-only sm:not-sr-only">{STATUS_LABEL[connection]}</span>
                        </span>
                        <span className="md:hidden">
                            <ThemeToggle />
                        </span>
                        <button
                            type="button"
                            onClick={handleLogout}
                            className="text-sm text-muted underline-offset-4 hover:text-ink hover:underline md:hidden"
                        >
                            Đăng xuất
                        </button>
                    </div>
                </header>

                <div className="px-4 pb-3">
                    <label className="relative block">
                        <span className="sr-only">Tìm cuộc trò chuyện</span>
                        <span className="pointer-events-none absolute inset-y-0 left-3 grid place-items-center text-muted">
                            <Icon d={ICONS.search} size={16} />
                        </span>
                        <input
                            type="search"
                            value={query}
                            onChange={(e) => setQuery(e.target.value)}
                            placeholder="Tìm kiếm"
                            className="h-10 w-full rounded-md border border-line bg-paper pl-9 pr-3 text-sm text-ink outline-none transition-colors placeholder:text-muted focus:border-primary"
                        />
                    </label>
                </div>

                <div className="flex gap-2 px-4 pb-3">
                    <button
                        type="button"
                        onClick={() => setShowDirect((v) => !v)}
                        aria-expanded={showDirect}
                        className={`flex h-9 flex-1 items-center justify-center gap-1.5 rounded-md border text-sm transition-colors ${
                            showDirect ? "border-primary bg-tint text-primary" : "border-line hover:bg-tint"
                        }`}
                    >
                        <Icon d={ICONS.plus} size={15} />
                        Nhắn riêng
                    </button>
                    <button
                        type="button"
                        onClick={() => setShowCreate((v) => !v)}
                        aria-expanded={showCreate}
                        className={`flex h-9 flex-1 items-center justify-center gap-1.5 rounded-md border text-sm transition-colors ${
                            showCreate ? "border-primary bg-tint text-primary" : "border-line hover:bg-tint"
                        }`}
                    >
                        <Icon d={ICONS.plus} size={15} />
                        Nhóm mới
                    </button>
                </div>

                {showDirect ? (
                    <form onSubmit={startDirect} className="flex gap-2 px-4 pb-3">
                        <input
                            value={directName}
                            onChange={(e) => setDirectName(e.target.value)}
                            placeholder="Tên đăng nhập người nhận"
                            aria-label="Tên đăng nhập người nhận"
                            autoCapitalize="none"
                            spellCheck={false}
                            className="h-9 min-w-0 flex-1 rounded-md border border-line bg-paper px-3 text-sm text-ink outline-none placeholder:text-muted focus:border-primary"
                        />
                        <button
                            type="submit"
                            disabled={directBusy || !directName.trim()}
                            className="h-9 rounded-md bg-primary px-4 text-sm font-medium text-primary-ink transition-colors hover:bg-primary-hover disabled:opacity-40"
                        >
                            Mở
                        </button>
                    </form>
                ) : null}

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

                <ul className="min-h-0 flex-1 overflow-y-auto px-2 pb-4">
                    {visible.length === 0 ? (
                        <li className="px-3 py-8 text-center text-sm text-muted">
                            {query ? "Không có cuộc trò chuyện phù hợp." : "Chưa có cuộc trò chuyện nào."}
                        </li>
                    ) : null}
                    {visible.map((c) => {
                        const isActive = c.id === activeId;
                        return (
                            <li key={c.id}>
                                <button
                                    type="button"
                                    onClick={() => openConversation(c.id)}
                                    aria-current={isActive ? "true" : undefined}
                                    className={`flex w-full items-center gap-3 rounded-md px-3 py-2.5 text-left transition-colors hover:bg-paper ${
                                        isActive ? "bg-tint hover:bg-tint" : ""
                                    }`}
                                >
                                    <Avatar title={c.title} group={c.kind === "group"} />
                                    <span className="min-w-0 flex-1">
                                        <span className="flex items-baseline justify-between gap-2">
                                            <span className="flex min-w-0 items-center gap-1.5">
                                                <span className="truncate font-medium">{c.title}</span>
                                                <span className="shrink-0 rounded px-1 text-[10px] leading-4 text-muted ring-1 ring-line">
                                                    {c.kind === "group" ? "Nhóm" : "Riêng"}
                                                </span>
                                            </span>
                                            <span className="shrink-0 whitespace-nowrap text-xs text-muted">
                                                {formatChatTime(c.lastTime)}
                                            </span>
                                        </span>
                                        <span className="mt-0.5 flex items-center justify-between gap-2">
                                            <span className="truncate text-sm text-muted">
                                                {c.lastMessage || "Chưa có tin nhắn"}
                                            </span>
                                            {c.unread > 0 ? (
                                                <span className="shrink-0 rounded-full bg-primary px-2 text-xs font-medium leading-5 text-primary-ink">
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

            <main className={`${active ? "flex" : "hidden md:flex"} min-w-0 flex-1 flex-col bg-paper`}>
                {active ? (
                    <>
                        <header className="flex items-center justify-between gap-3 border-b border-line bg-surface px-4 py-3 sm:px-6">
                            <div className="flex min-w-0 items-center gap-3">
                                <button
                                    type="button"
                                    onClick={() => setActiveId(null)}
                                    aria-label="Quay lại danh sách"
                                    className="grid size-9 shrink-0 place-items-center rounded-md text-primary transition-colors hover:bg-tint md:hidden"
                                >
                                    <Icon d={ICONS.back} />
                                </button>
                                <Avatar title={active.title} group={active.kind === "group"} size="sm" />
                                <div className="min-w-0">
                                    <h2 className="truncate text-base font-semibold">{active.title}</h2>
                                    <p className="truncate text-xs text-muted">
                                        {active.kind === "group" ? "Nhóm học tập" : "Trò chuyện trực tiếp"}
                                    </p>
                                </div>
                            </div>
                            {active.kind === "group" ? (
                                <button
                                    type="button"
                                    aria-pressed={showInfo}
                                    onClick={() => setShowInfo((v) => !v)}
                                    className={`hidden shrink-0 items-center gap-1.5 rounded-md border px-3 py-1.5 text-sm transition-colors lg:flex ${
                                        showInfo ? "border-primary bg-tint text-primary" : "border-line hover:bg-tint"
                                    }`}
                                >
                                    <Icon d={ICONS.users} size={16} />
                                    {showInfo ? "Ẩn thông tin" : "Thông tin nhóm"}
                                </button>
                            ) : null}
                            {active.kind === "group" ? (
                                <button
                                    type="button"
                                    aria-pressed={showMembers}
                                    onClick={() => setShowMembers((v) => !v)}
                                    className={`flex shrink-0 items-center gap-1.5 rounded-md border px-3 py-1.5 text-sm transition-colors lg:hidden ${
                                        showMembers ? "border-primary bg-tint text-primary" : "border-line hover:bg-tint"
                                    }`}
                                >
                                    <Icon d={ICONS.users} size={16} />
                                    Thành viên
                                </button>
                            ) : null}
                        </header>

                        <div className="lg:hidden">
                            {showMembers && active.kind === "group" ? (
                                activeMembers ? (
                                    <MembersPanel
                                        groupId={active.id}
                                        members={activeMembers}
                                        isOwner={isOwner}
                                        currentUserId={me}
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
                        </div>

                        <div className="flex min-h-0 flex-1">
                            <div className="flex min-w-0 flex-1 flex-col">
                                <MessageList
                                    key={active.id}
                                    messages={activeMessages}
                                    currentUserId={me}
                                    isGroup={active.kind === "group"}
                                />
                                <Composer onSend={sendMessage} />
                            </div>
                            {active.kind === "group" ? (
                                <aside
                                    aria-label="Thông tin nhóm"
                                    className={`hidden shrink-0 overflow-hidden border-l border-line bg-surface transition-[width] duration-200 lg:flex ${
                                        showInfo ? "lg:w-72" : "lg:w-0 lg:border-l-0"
                                    }`}
                                >
                                    <div className="flex w-72 min-h-0 flex-1 flex-col overflow-y-auto [&>section]:max-h-none [&>section]:border-b-0">
                                        {activeMembers ? (
                                            <MembersPanel
                                                groupId={active.id}
                                                members={activeMembers}
                                                isOwner={isOwner}
                                                currentUserId={me}
                                                onLeft={handleLeft}
                                                onChanged={() => setMembersVersion((v) => v + 1)}
                                                onNotice={showNotice}
                                            />
                                        ) : (
                                            <div className="px-4 py-3 text-sm text-muted">Đang tải thành viên...</div>
                                        )}
                                    </div>
                                </aside>
                            ) : null}
                        </div>
                    </>
                ) : (
                    <div className="m-auto flex max-w-xs flex-col items-center gap-3 px-6 text-center">
                        <span className="grid size-14 place-items-center rounded-lg bg-tint text-primary">
                            <Icon d={ICONS.chat} size={26} />
                        </span>
                        <p className="font-serif text-lg font-semibold">Chọn một cuộc trò chuyện</p>
                        <p className="text-sm text-muted">
                            Chọn một đoạn chat bên trái, hoặc tạo nhóm mới để bắt đầu.
                        </p>
                    </div>
                )}
            </main>
            {notice ? (
                <div
                    role="status"
                    className={`fixed top-4 left-1/2 z-50 md:top-6 flex w-[min(92vw,28rem)] -translate-x-1/2 items-center justify-between gap-3 rounded-md border border-line bg-surface px-4 py-2.5 text-sm shadow-[0_12px_32px_-12px_rgba(13,37,56,0.35)] ${
                        notice.tone === "error" ? "text-danger" : "text-primary"
                    }`}
                >
                    <span>{notice.text}</span>
                    <button
                        type="button"
                        onClick={() => setNotice(null)}
                        aria-label="Đóng thông báo"
                        className="grid size-7 shrink-0 place-items-center rounded-md text-muted hover:bg-paper hover:text-ink"
                    >
                        <Icon d="M18 6L6 18M6 6l12 12" size={16} />
                    </button>
                </div>
            ) : null}
        </div>
    );
}