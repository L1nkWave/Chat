import { listClasses, listFaculties } from "@/lib/admin/catalog-api";
import { searchUsers } from "@/lib/admin/users-api";
import type { PickerOption } from "./EntityPicker";

// Hàm tìm dùng chung cho EntityPicker. Khai báo ở đây để tham chiếu không đổi giữa các lần render.

export async function searchPeople(query: string): Promise<PickerOption[]> {
    const page = await searchUsers({ q: query, role: "", status: "ALL", faculty: "", classCode: "", subject: "", page: 0, size: 20 });
    return page.items.map((user) => ({ id: user.id, label: user.name, hint: `@${user.username}` }));
}

export async function searchFaculties(query: string): Promise<PickerOption[]> {
    const items = await listFaculties(query);
    return items.map((faculty) => ({ id: faculty.id, label: faculty.name, hint: faculty.code }));
}

export async function searchClasses(query: string): Promise<PickerOption[]> {
    const items = await listClasses(query, null);
    return items.map((item) => ({
        id: item.id,
        label: item.name,
        hint: [item.code, item.facultyName, item.cohort].filter(Boolean).join(" · "),
    }));
}