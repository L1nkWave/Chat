import { apiFetch } from "@/lib/api/client";

export type Faculty = {
    id: number;
    code: string;
    name: string;
};

export type SchoolClass = {
    id: number;
    code: string;
    name: string;
    facultyId: number;
    facultyName: string;
    cohort: string | null;
    advisorId: number | null;
};

export function listFaculties(q: string): Promise<Faculty[]> {
    const params = new URLSearchParams({ q });
    return apiFetch<Faculty[]>(`/api/v1/admin/catalog/faculties?${params.toString()}`);
}

export function createFaculty(code: string, name: string): Promise<Faculty> {
    return apiFetch<Faculty>("/api/v1/admin/catalog/faculties", {
        method: "POST",
        body: JSON.stringify({ code, name }),
    });
}

/** facultyId null nghĩa là tất cả khoa */
export function listClasses(q: string, facultyId: number | null): Promise<SchoolClass[]> {
    const params = new URLSearchParams({ q });
    if (facultyId !== null) params.set("facultyId", String(facultyId));
    return apiFetch<SchoolClass[]>(`/api/v1/admin/catalog/classes?${params.toString()}`);
}

export function createClass(input: {
    code: string;
    name: string;
    facultyId: number;
    cohort: string;
    advisorId: number | null;
}): Promise<SchoolClass> {
    return apiFetch<SchoolClass>("/api/v1/admin/catalog/classes", {
        method: "POST",
        body: JSON.stringify({
            code: input.code,
            name: input.name,
            facultyId: input.facultyId,
            cohort: input.cohort || null,
            advisorId: input.advisorId,
        }),
    });
}