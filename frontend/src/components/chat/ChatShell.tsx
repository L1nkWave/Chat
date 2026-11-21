"use client";

import { Fragment, useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { Logo } from "@/components/brand/Logo";
import { ThemeToggle } from "@/components/theme/ThemeToggle";
import { ApiError, apiFetch, refreshAccessToken } from "@/lib/api/client";
import { isPasswordChangeRequired } from "@/lib/auth/session";
import { logout } from "@/lib/auth/auth-api";
import { setAccessToken } from "@/lib/auth/session";
import { chatApi, errorMessage } from "@/lib/chat/api";
import { uploadFile, type CreatedFile } from "@/lib/chat/files";
import { formatChatTime } from "@/lib/chat/time";
import { sortConversations, timeValue } from "@/lib/chat/sort";
import { CATEGORY_FILTERS, matchesCategory, type CategoryFilter } from "@/lib/chat/filters";
import type { Conversation, Invitation, Member, Message, UserSuggestion } from "@/lib/chat/types";
import { ChatSocket, type ConnectionStatus } from "@/lib/ws/chat-socket";
import type { ChatEvent } from "@/lib/ws/protocol";
import { EMPTY, FIELD, badgeStyle, buttonStyle } from "@/components/ui/styles";
import { Composer } from "./Composer";
import { ConversationPanel } from "./ConversationPanel";
import { CreateGroupForm, InvitationList } from "./GroupPanels";
import { DirectChatSearch } from "./DirectChatSearch";
import { MessageList } from "./MessageList";

type TokensDto = { accessToken: string; refreshExpiration: number };
type Notice = { text: string; tone: "error" | "info" };

const STATUS_LABEL: Record<ConnectionStatus, string> = {
    connecting: "Đang kết nối...",
    open: "Đã kết nối",
    reconnecting: "Mất kết nối, đang thử lại...",
    closed: "Đã ngắt kết nối",
};

const PAGE_SIZE = 50;

// Nút lọc dạng chip: trạng thái chọn lấy từ aria-pressed, nên giao diện luôn khớp với trạng thái
const CHIP =
    "h-8 rounded-full border border-line px-3 text-xs text-muted transition-colors hover:bg-tint hover:text-ink aria-pressed:border-primary aria-pressed:bg-tint aria-pressed:font-medium aria-pressed:text-primary";

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
    pin: "M9 3h6l-1 6 3 3H7l3-3-1-6zM12 15v6",
    user: "M12 11a4 4 0 100-8 4 4 0 000 8zM4 21c0-4 3.6-7 8-7s8 3 8 7",
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

function SectionLabel({ children }: { children: ReactNode }) {
    return (
        <li className="px-3 pb-1 pt-3 text-xs font-medium text-muted">{children}</li>
    );
}

export function ChatShell() {
    const router = useRouter();
    const [ready, setReady] = useState(false);
    const [me, setMe] = useState("");
    const [connection, setConnection] = useState<ConnectionStatus>("connecting");
    const [notice, setNotice] = useState<Notice | null>(null);
    const [conversations, setConversations] = useState<Conversation[]>([]);
    // Chat riêng vừa mở lại từ "Nhắn riêng" khi đang bị ẩn ở server: giữ trong danh sách cho tới khi ẩn lại
    const reopenedIds = useRef(new Set<string>());
    const [invitations, setInvitations] = useState<Invitation[]>([]);
    const [messages, setMessages] = useState<Record<string, Message[]>>({});
    const [membersState, setMembersState] = useState<{
        groupId: string;
        list: Member[];
    } | null>(null);
    const [membersVersion, setMembersVersion] = useState(0);
    // Panel thông tin bên phải (trên điện thoại là ngăn kéo)
    const [panelOpen, setPanelOpen] = useState(false);
    const [categoryFilter, setCategoryFilter] = useState<CategoryFilter>("ALL");
    const [pinBusyId, setPinBusyId] = useState<string | null>(null);
    // Còn tin cũ hơn để tải không, theo từng cuộc trò chuyện
    const [hasOlder, setHasOlder] = useState<Record<string, boolean>>({});
    const [loadingOlder, setLoadingOlder] = useState(false);
    const [showCreate, setShowCreate] = useState(false);
    const [activeId, setActiveId] = useState<string | null>(null);
    const [query, setQuery] = useState("");
    const [profile, setProfile] = useState<{ name: string; username: string } | null>(null);
    const [showDirect, setShowDirect] = useState(false);
    const [directBusy, setDirectBusy] = useState(false);
    // Tăng khi đoạn chat đang mở có tin file mới hoặc tin bị xóa: tab Ảnh & file tải lại
    const [mediaVersion, setMediaVersion] = useState(0);

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
            const list = await chatApi.listConversations();
            const missing = [...reopenedIds.current].filter((id) => !list.some((c) => c.id === id));
            const extra = await Promise.all(
                missing.map((id) => chatApi.getConversation(id).catch(() => null)),
            );
            setConversations([...list, ...extra.filter((c): c is Conversation => c !== null)]);
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
                setHasOlder((prev) => ({ ...prev, [conversationId]: list.length >= PAGE_SIZE }));
            } catch (err) {
                showNotice(errorMessage(err), "error");
            }
        },
        [showNotice],
    );

    // Tải thêm tin cũ hơn. offset tính theo số tin đã có; tin trùng được bỏ qua theo id.
    const loadOlder = useCallback(async () => {
        if (!activeId || loadingOlder) return;
        const current = messages[activeId] ?? [];
        const known = new Set(current.map((m) => m.id));
        setLoadingOlder(true);
        try {
            const older = await chatApi.listMessages(activeId, current.length, PAGE_SIZE);
            const fresh = older.filter((m) => !known.has(m.id));
            setMessages((prev) => {
                const existing = prev[activeId] ?? [];
                const seen = new Set(existing.map((m) => m.id));
                const added = fresh.filter((m) => !seen.has(m.id));
                const merged = [...added, ...existing].sort((a, b) => timeValue(a.time) - timeValue(b.time));
                return { ...prev, [activeId]: merged };
            });
            setHasOlder((prev) => ({ ...prev, [activeId]: fresh.length > 0 }));
        } catch (err) {
            showNotice(errorMessage(err), "error");
        } finally {
            setLoadingOlder(false);
        }
    }, [activeId, messages, loadingOlder, showNotice]);

    // Sau khi kết nối lại, tải lại toàn bộ trạng thái để bù các tin và sự kiện đã bỏ lỡ.
    const resync = useCallback(() => {
        void loadConversations();
        void loadInvitations();
        const id = activeIdRef.current;
        if (id) {
            void loadMessages(id);
            setMediaVersion((v) => v + 1);
        }
    }, [loadConversations, loadInvitations, loadMessages]);

    // Khôi phục phiên từ refresh cookie. Dùng chung promise làm mới với các request khác,
    // nên chế độ StrictMode (chạy effect hai lần) không gửi cùng một refresh token hai lần.
    useEffect(() => {
        let cancelled = false;
        void refreshAccessToken().then((ok) => {
            if (cancelled) return;
            if (!ok) {
                router.replace("/login");
                return;
            }
            if (isPasswordChangeRequired()) {
                router.replace("/change-password");
                return;
            }
            setReady(true);
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
            if (message.attachment && message.conversationId === activeIdRef.current) {
                setMediaVersion((v) => v + 1);
            }
            setMessages((prev) => {
                const list = prev[message.conversationId] ?? [];
                // Bỏ qua tin trùng, vì tin có thể vừa được tải lại khi kết nối lại.
                if (list.some((m) => m.id === message.id)) return prev;
                return {
                    ...prev,
                    [message.conversationId]: [...list, message].sort((a, b) => timeValue(a.time) - timeValue(b.time)),
                };
            });
            setConversations((prev) => {
                const target = prev.find((c) => c.id === message.conversationId);
                if (!target) {
                    // Đoạn chat đã ẩn vừa có tin mới: tải lại danh sách để hiện lại
                    void loadConversations();
                    return prev;
                }
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
                } else if (event.type === "messageUpdated") {
                    setMessages((prev) => ({
                        ...prev,
                        [event.conversationId]: (prev[event.conversationId] ?? []).map((m) =>
                            m.id === event.messageId ? { ...m, text: event.text, edited: true, editedAt: event.editedAt ?? m.editedAt } : m,
                        ),
                    }));
                } else if (event.type === "messageRemoved") {
                    if (event.conversationId === activeIdRef.current) setMediaVersion((v) => v + 1);
                    setMessages((prev) => ({
                        ...prev,
                        [event.conversationId]: (prev[event.conversationId] ?? []).map((m) =>
                            m.id === event.messageId
                                ? { ...m, text: "", removedAt: event.removedAt ?? new Date().toISOString() }
                                : m,
                        ),
                    }));
                } else if (event.type === "historyCleared") {
                    // Đoạn chat bị ẩn ở phía mình: bỏ khỏi danh sách, không ảnh hưởng người khác
                    setMessages((prev) => {
                        const next = { ...prev };
                        delete next[event.conversationId];
                        return next;
                    });
                    setConversations((prev) => prev.filter((c) => c.id !== event.conversationId));
                    setActiveId((current) => (current === event.conversationId ? null : current));
                } else if (event.type === "invitation") {
                    void loadInvitations();
                } else if (event.type === "chatDeleted") {
                    // Nhóm bị xóa (bởi chủ nhóm, kể cả khi mình là người xóa): bỏ khỏi danh sách
                    setMessages((prev) => {
                        const next = { ...prev };
                        delete next[event.conversationId];
                        return next;
                    });
                    setActiveId((current) => (current === event.conversationId ? null : current));
                    setPanelOpen(false);
                    showNotice("Nhóm đã bị xóa.", "info");
                    void loadConversations();
                } else if (event.type === "membership") {
                    if (event.kicked) {
                        // Bị xóa khỏi nhóm: bỏ lịch sử đã cache và đóng khung chat ngay
                        setMessages((prev) => {
                            const next = { ...prev };
                            delete next[event.conversationId];
                            return next;
                        });
                        setActiveId((current) => (current === event.conversationId ? null : current));
                        setPanelOpen(false);
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

    // Tin đến qua WebSocket không kèm tên người gửi, nên lấy tên từ danh sách thành viên
    const displayMessages = useMemo(() => {
        const names = new Map((activeMembers ?? []).map((m) => [m.userId, m.name]));
        return activeMessages.map((m) =>
            m.senderName ? m : { ...m, senderName: names.get(m.senderId) ?? "Thành viên" },
        );
    }, [activeMessages, activeMembers]);

    const visible = useMemo(() => {
        const q = query.trim().toLowerCase();
        return sortConversations(
            conversations.filter((c) => matchesCategory(c, categoryFilter) && c.title.toLowerCase().includes(q)),
        );
    }, [conversations, query, categoryFilter]);

    // Mục "Đã ghim" đứng sau bot (sortConversations đã sắp xếp)
    const hasPinned = visible.some((c) => c.pinned);
    const firstUnpinned = visible.findIndex((c) => !c.pinned);

    function openConversation(id: string) {
        setNotice(null);
        setPanelOpen(false);
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

    // Xóa nhóm đang mở qua WebSocket; server phát CHAT_DELETED tới mọi thành viên
    function deleteActiveGroup() {
        if (!activeId) return;
        const sent = socketRef.current?.deleteGroup(activeId) ?? false;
        if (!sent) showNotice("Chưa kết nối được tới máy chủ. Vui lòng thử lại sau.", "error");
    }

    // Sửa, xóa tin và xóa đoạn chat qua WebSocket; server phát sự kiện tới các thành viên
    function editMessage(messageId: string, text: string) {
        const sent = socketRef.current?.editMessage(messageId, text) ?? false;
        if (!sent) showNotice("Chưa kết nối được tới máy chủ. Vui lòng thử lại sau.", "error");
    }

    function removeMessage(messageId: string) {
        const sent = socketRef.current?.removeMessage(messageId) ?? false;
        if (!sent) showNotice("Chưa kết nối được tới máy chủ. Vui lòng thử lại sau.", "error");
    }

    function clearActiveHistory() {
        if (!activeId) return;
        if (!window.confirm("Ẩn đoạn chat này khỏi danh sách của bạn? Tin nhắn vẫn được giữ, đoạn chat sẽ hiện lại khi có tin mới.")) return;
        // Ẩn ở phía mình ngay; phía đối phương không bị ảnh hưởng
        const hiddenId = activeId;
        reopenedIds.current.delete(hiddenId);
        setConversations((prev) => prev.filter((c) => c.id !== hiddenId));
        setMessages((prev) => {
            const next = { ...prev };
            delete next[hiddenId];
            return next;
        });
        setActiveId(null);
        const sent = socketRef.current?.clearHistory(hiddenId) ?? false;
        if (!sent) showNotice("Chưa kết nối được tới máy chủ. Vui lòng thử lại sau.", "error");
    }

    // Ghim hoặc bỏ ghim rồi tải lại danh sách. Server giới hạn 5 cuộc ghim; lỗi (422) hiện thông báo từ server.
    async function togglePin(target: Conversation) {
        if (target.isBot || pinBusyId) return;
        setPinBusyId(target.id);
        try {
            await chatApi.setPinned(target.id, !target.pinned);
            await loadConversations();
        } catch (err) {
            showNotice(errorMessage(err), "error");
        } finally {
            setPinBusyId(null);
        }
    }

    async function reportMessage(messageId: string, category: string, reason?: string) {
        try {
            await chatApi.reportMessage(messageId, category, reason);
            showNotice("Đã gửi báo cáo. Cảm ơn bạn đã góp phần giữ môi trường học tập lành mạnh.", "info");
        } catch (err) {
            showNotice(
                err instanceof ApiError ? err.message : "Không gửi được báo cáo. Vui lòng thử lại.",
                "error",
            );
        }
    }

    function handleLeft() {
        setPanelOpen(false);
        setActiveId(null);
        void loadConversations();
    }

    // Gửi một tệp thành một tin nhắn: tải lên qua REST rồi báo server qua WebSocket /send_file
    // để phát tới người nhận. Trả về thông báo lỗi, hoặc null nếu đã gửi.
    async function sendFile(file: File): Promise<string | null> {
        if (!activeId) return "Chưa chọn cuộc trò chuyện.";
        const socket = socketRef.current;
        if (!socket?.isOpen()) return "Chưa kết nối được tới máy chủ.";
        let created: CreatedFile;
        try {
            created = await uploadFile(activeId, file);
        } catch (err) {
            return errorMessage(err);
        }
        if (!socket.sendFile(activeId, created)) return "Mất kết nối trước khi gửi.";
        return null;
    }

    // Tin chữ chỉ hiện khi server phát lại qua sự kiện MESSAGE
    async function sendText(text: string): Promise<boolean> {
        if (!activeId) return false;
        const socket = socketRef.current;
        if (!socket?.isOpen()) {
            showNotice("Chưa kết nối được tới máy chủ. Tin nhắn chưa được gửi.", "error");
            return false;
        }
        if (!socket.send(activeId, text)) {
            showNotice("Mất kết nối trước khi gửi. Vui lòng thử lại.", "error");
            return false;
        }
        return true;
    }

    // Mở chat riêng với người đã chọn từ gợi ý (hoặc mở lại chat cũ)
    async function startDirect(user: UserSuggestion) {
        if (directBusy) return;
        setDirectBusy(true);
        try {
            const conversation = await chatApi.startDirectChat(user.id);
            reopenedIds.current.add(conversation.id);
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
        <div className="flex h-dvh flex-col bg-paper text-ink">
            <div className="flex min-h-0 flex-1">
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
                        onClick={() => router.push("/account")}
                        aria-label="Tài khoản"
                        title="Tài khoản"
                        className="grid size-10 place-items-center rounded-md text-muted transition-colors hover:bg-tint hover:text-ink"
                    >
                        <svg viewBox="0 0 24 24" width={20} height={20} fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" aria-hidden="true">
                            <circle cx="12" cy="8" r="4" />
                            <path d="M4 21c0-4 3.6-7 8-7s8 3 8 7" />
                        </svg>
                    </button>
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
                    <header className="flex h-16 shrink-0 items-center justify-between gap-3 border-b border-line px-5">
                        <div className="min-w-0">
                            <h1 className="text-lg font-semibold leading-tight">Tin nhắn</h1>
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
                        </div>
                    </header>

                    <div className="px-4 py-3">
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
                                className={`${FIELD} pl-9`}
                            />
                        </label>
                    </div>

                    <div role="group" aria-label="Lọc theo loại" className="flex flex-wrap gap-1.5 px-4 pb-3">
                        {CATEGORY_FILTERS.map((f) => (
                            <button
                                key={f.value}
                                type="button"
                                aria-pressed={categoryFilter === f.value}
                                onClick={() => setCategoryFilter(f.value)}
                                className={CHIP}
                            >
                                {f.label}
                            </button>
                        ))}
                    </div>

                    <div className="flex gap-2 px-4 pb-3">
                        <button
                            type="button"
                            onClick={() => setShowDirect((v) => !v)}
                            aria-expanded={showDirect}
                            className={buttonStyle("secondary", "md", "flex-1 aria-expanded:border-primary aria-expanded:bg-tint aria-expanded:text-primary")}
                        >
                            <Icon d={ICONS.plus} size={15} />
                            Nhắn riêng
                        </button>
                        <button
                            type="button"
                            onClick={() => setShowCreate((v) => !v)}
                            aria-expanded={showCreate}
                            className={buttonStyle("secondary", "md", "flex-1 aria-expanded:border-primary aria-expanded:bg-tint aria-expanded:text-primary")}
                        >
                            <Icon d={ICONS.plus} size={15} />
                            Nhóm mới
                        </button>
                    </div>

                    {showDirect ? <DirectChatSearch busy={directBusy} onPick={(user) => void startDirect(user)} /> : null}

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
                            <li className={EMPTY}>
                                {query || categoryFilter !== "ALL" ? "Không có cuộc trò chuyện phù hợp." : "Chưa có cuộc trò chuyện nào."}
                            </li>
                        ) : null}
                        {visible.map((c, index) => {
                            const isActive = c.id === activeId;
                            return (
                                <Fragment key={c.id}>
                                    {hasPinned && index === 0 ? <SectionLabel>Đã ghim</SectionLabel> : null}
                                    {hasPinned && index === firstUnpinned ? (
                                        <SectionLabel>Các cuộc trò chuyện khác</SectionLabel>
                                    ) : null}
                                    <li className="flex items-center gap-1">
                                        <button
                                            type="button"
                                            onClick={() => openConversation(c.id)}
                                            aria-current={isActive ? "true" : undefined}
                                            className={`flex min-w-0 flex-1 items-center gap-3 rounded-md px-3 py-2.5 text-left transition-colors hover:bg-paper ${
                                                isActive ? "bg-tint hover:bg-tint" : ""
                                            }`}
                                        >
                                            <Avatar title={c.title} group={c.kind === "group"} />
                                            <span className="min-w-0 flex-1">
                                                <span className="flex items-baseline justify-between gap-2">
                                                    <span className="flex min-w-0 items-center gap-1.5">
                                                        <span className={`truncate text-[15px] ${c.unread > 0 ? "font-semibold" : "font-medium"}`}>{c.title}</span>
                                                        <span className={badgeStyle("outline")}>
                                                            {c.kind === "group" ? "Nhóm" : "Riêng"}
                                                        </span>
                                                    </span>
                                                    <span className="shrink-0 whitespace-nowrap text-xs tabular-nums text-muted">
                                                        {formatChatTime(c.lastTime)}
                                                    </span>
                                                </span>
                                                <span className="mt-0.5 flex items-center justify-between gap-2">
                                                    <span className="truncate text-[15px] text-muted">
                                                        {c.lastMessage || "Chưa có tin nhắn"}
                                                    </span>
                                                    {c.unread > 0 ? (
                                                        <span className="shrink-0 rounded-full bg-primary px-1.5 text-xs font-semibold leading-5 tabular-nums text-primary-ink">
                                                            {c.unread}
                                                        </span>
                                                    ) : null}
                                                </span>
                                            </span>
                                        </button>
                                        {c.isBot ? null : (
                                            <button
                                                type="button"
                                                onClick={() => void togglePin(c)}
                                                disabled={pinBusyId !== null}
                                                aria-pressed={c.pinned}
                                                aria-label={c.pinned ? `Bỏ ghim ${c.title}` : `Ghim ${c.title}`}
                                                title={c.pinned ? "Bỏ ghim" : "Ghim lên đầu"}
                                                className={`grid size-9 shrink-0 place-items-center rounded-md transition-colors hover:bg-paper disabled:opacity-40 ${
                                                    c.pinned ? "text-primary" : "text-muted hover:text-ink"
                                                }`}
                                            >
                                                <Icon d={ICONS.pin} size={16} />
                                            </button>
                                        )}
                                    </li>
                                </Fragment>
                            );
                        })}
                    </ul>
                </aside>

                <main className={`${active ? "flex" : "hidden md:flex"} min-w-0 flex-1 flex-col bg-paper`}>
                    {active ? (
                        <>
                            <header className="flex h-16 shrink-0 items-center justify-between gap-3 border-b border-line bg-surface px-4 sm:px-6">
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
                                {active.kind === "direct" && !active.isBot ? (
                                    <button
                                        type="button"
                                        onClick={clearActiveHistory}
                                        className={buttonStyle("secondary", "md", "hidden lg:inline-flex")}
                                    >
                                        Ẩn đoạn chat
                                    </button>
                                ) : null}
                                <button
                                    type="button"
                                    aria-pressed={panelOpen}
                                    onClick={() => setPanelOpen((v) => !v)}
                                    className={buttonStyle("secondary", "md", "aria-pressed:border-primary aria-pressed:bg-tint aria-pressed:text-primary")}
                                >
                                    <Icon d={ICONS.users} size={16} />
                                    Thông tin
                                </button>
                            </header>

                            <div className="relative flex min-h-0 flex-1">
                                <div className="flex min-w-0 flex-1 flex-col">
                                    <MessageList
                                        key={active.id}
                                        messages={displayMessages}
                                        currentUserId={me}
                                        isGroup={active.kind === "group"}
                                        onEdit={editMessage}
                                        onRemove={removeMessage}
                                        onReport={reportMessage}
                                        hasOlder={hasOlder[active.id] ?? false}
                                        loadingOlder={loadingOlder}
                                        onLoadOlder={() => void loadOlder()}
                                    />
                                    <Composer onSendFile={sendFile} onSendText={sendText} />
                                </div>
                                {panelOpen ? (
                                    <>
                                        <button
                                            type="button"
                                            aria-label="Đóng thông tin"
                                            onClick={() => setPanelOpen(false)}
                                            className="absolute inset-0 z-20 bg-ink/30 lg:hidden"
                                        />
                                        <aside
                                            aria-label="Thông tin cuộc trò chuyện"
                                            className="absolute inset-y-0 right-0 z-30 flex w-[min(92vw,20rem)] flex-col border-l border-line shadow-lg lg:static lg:z-auto lg:w-72 lg:shrink-0 lg:shadow-none"
                                        >
                                            <ConversationPanel
                                                key={active.id}
                                                conversation={active}
                                                members={activeMembers}
                                                currentUserId={me}
                                                isOwner={isOwner}
                                                onClose={() => setPanelOpen(false)}
                                                onLeft={handleLeft}
                                                onMembersChanged={() => setMembersVersion((v) => v + 1)}
                                                onNotice={showNotice}
                                                onDeleteGroup={deleteActiveGroup}
                                                mediaVersion={mediaVersion}
                                                onRemoveMessage={removeMessage}
                                            />
                                        </aside>
                                    </>
                                ) : null}
                            </div>
                        </>
                    ) : (
                        <div className="m-auto flex max-w-xs flex-col items-center gap-3 px-6 text-center">
                            <span className="grid size-14 place-items-center rounded-lg bg-tint text-primary">
                                <Icon d={ICONS.chat} size={26} />
                            </span>
                            <p className="text-base font-semibold">Chọn một cuộc trò chuyện</p>
                            <p className="text-sm text-muted">
                                Chọn một đoạn chat bên trái, hoặc tạo nhóm mới để bắt đầu.
                            </p>
                        </div>
                    )}
                </main>
            </div>
            <nav
                aria-label="Điều hướng nhanh"
                className="flex shrink-0 items-center justify-around border-t border-line bg-surface px-2 py-1.5 md:hidden"
            >
                <button
                    type="button"
                    onClick={() => {
                        setPanelOpen(false);
                        setActiveId(null);
                    }}
                    aria-label="Tin nhắn"
                    className="flex min-w-14 flex-col items-center gap-0.5 rounded-md px-2 py-1 text-xs text-primary transition-colors hover:bg-tint"
                >
                    <Icon d={ICONS.chat} size={20} />
                    Tin nhắn
                </button>
                <ThemeToggle />
                <button
                    type="button"
                    onClick={() => router.push("/account")}
                    aria-label="Tài khoản"
                    className="flex min-w-14 flex-col items-center gap-0.5 rounded-md px-2 py-1 text-xs text-muted transition-colors hover:bg-tint hover:text-ink"
                >
                    <Icon d={ICONS.user} size={20} />
                    Tài khoản
                </button>
                <button
                    type="button"
                    onClick={handleLogout}
                    aria-label="Đăng xuất"
                    className="flex min-w-14 flex-col items-center gap-0.5 rounded-md px-2 py-1 text-xs text-muted transition-colors hover:bg-tint hover:text-ink"
                >
                    <Icon d={ICONS.logout} size={20} />
                    Đăng xuất
                </button>
            </nav>
            {notice ? (
                <div
                    role="status"
                    className={`fixed top-4 left-1/2 z-50 md:top-6 flex w-[min(92vw,28rem)] -translate-x-1/2 items-center justify-between gap-3 rounded-md border border-line bg-surface px-4 py-2.5 text-sm shadow-md ${
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