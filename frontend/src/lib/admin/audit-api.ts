import { apiFetch } from "@/lib/api/client";

export type AuditRow = {
    id: number;
    actorId: number;
    action: string;
    targetType: string | null;
    targetId: string | null;
    detail: string | null;
    ip: string | null;
    createdAt: string;
};

export type AuditPage = {
    items: AuditRow[];
    total: number;
    page: number;
    size: number;
};

export type AuditFilter = {
    action: string;
    actorId: string;
    targetId: string;
    from: string;
    to: string;
    page: number;
    size: number;
};

// from/to dạng yyyy-MM-dd (giờ Việt Nam), bao gồm cả ngày kết thúc
export function searchAudit(filter: AuditFilter): Promise<AuditPage> {
    const params = new URLSearchParams({
        action: filter.action,
        targetId: filter.targetId,
        page: String(filter.page),
        size: String(filter.size),
    });
    if (filter.actorId) params.set("actorId", filter.actorId);
    if (filter.from) params.set("from", filter.from);
    if (filter.to) params.set("to", filter.to);
    return apiFetch<AuditPage>(`/api/v1/admin/audit-logs?${params.toString()}`);
}