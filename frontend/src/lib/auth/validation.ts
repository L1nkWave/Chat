/**
 * Client-side checks give quick feedback. The server validates again and is the
 * authority, so these rules must never be the only check.
 */

export type LoginFields = {
    username: string;
    password: string;
};

export type SignupFields = {
    name: string;
    username: string;
    password: string;
    confirmPassword: string;
};

export type FieldErrors<K extends string> = Partial<Record<K, string>>;

const USERNAME_PATTERN = /^[A-Za-z0-9_.]{3,32}$/;

export function validateLogin(fields: LoginFields): FieldErrors<keyof LoginFields> {
    const errors: FieldErrors<keyof LoginFields> = {};
    if (!fields.username.trim()) {
        errors.username = "Vui lòng nhập tên đăng nhập.";
    }
    if (!fields.password) {
        errors.password = "Vui lòng nhập mật khẩu.";
    }
    return errors;
}

export function validateSignup(fields: SignupFields): FieldErrors<keyof SignupFields> {
    const errors: FieldErrors<keyof SignupFields> = {};

    const name = fields.name.trim();
    if (name.length < 3 || name.length > 64) {
        errors.name = "Họ và tên cần từ 3 đến 64 ký tự.";
    }

    if (!USERNAME_PATTERN.test(fields.username)) {
        errors.username =
            "Tên đăng nhập cần từ 3 đến 32 ký tự, gồm chữ không dấu, số, dấu gạch dưới hoặc dấu chấm.";
    }

    if (fields.password.length < 8 || fields.password.length > 64) {
        errors.password = "Mật khẩu cần từ 8 đến 64 ký tự.";
    } else if (!/[A-Za-z]/.test(fields.password) || !/\d/.test(fields.password)) {
        errors.password = "Mật khẩu cần có ít nhất một chữ cái và một chữ số.";
    }

    if (fields.confirmPassword !== fields.password) {
        errors.confirmPassword = "Mật khẩu nhập lại chưa khớp.";
    }

    return errors;
}

export function hasErrors<K extends string>(errors: FieldErrors<K>): boolean {
    return Object.keys(errors).length > 0;
}