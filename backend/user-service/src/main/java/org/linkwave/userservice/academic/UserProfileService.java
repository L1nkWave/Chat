package org.linkwave.userservice.academic;

import lombok.RequiredArgsConstructor;
import org.linkwave.userservice.exception.UnacceptableRequestDataException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Hồ sơ cá nhân mở rộng: giới tính, ngày sinh, tình trạng, sđt, nơi sinh, quốc tịch, địa chỉ,
 * CCCD, dân tộc, tôn giáo.
 * CCCD, dân tộc và tôn giáo được mã hóa trước khi lưu.
 * Trong ProfileInput: trường nhạy cảm null nghĩa là giữ nguyên, chuỗi rỗng nghĩa là xóa.
 * Các trường thường luôn được ghi theo giá trị gửi lên (rỗng thì xóa).
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    public static final Set<String> GENDERS = Set.of("MALE", "FEMALE", "OTHER");

    /** Tình trạng học tập (sinh viên) hoặc làm việc (giảng viên, quản trị viên) */
    public static final Map<String, Set<String>> STATUSES_BY_ROLE = Map.of(
            "ROLE_STUDENT", Set.of("STUDYING", "GRADUATED", "SUSPENDED", "DROPPED_OUT"),
            "ROLE_LECTURER", Set.of("WORKING", "ON_LEAVE", "RETIRED"),
            "ROLE_ADMIN", Set.of("WORKING"));

    private static final Pattern PHONE = Pattern.compile("^(0|\\+84)\\d{9}$");
    private static final String DEFAULT_NATIONALITY = "Việt Nam";
    private static final int MAX_LONG_TEXT = 255;
    private static final int MAX_SHORT_TEXT = 64;

    /** Dữ liệu nhập từ form tạo hoặc sửa */
    public record ProfileInput(String gender, LocalDate birthDate, String status, String phone,
                               String birthplace, String nationality, String address,
                               String cccd, String ethnicity, String religion) {
    }

    /** Hồ sơ hiển thị: CCCD đã che, dân tộc và tôn giáo chỉ cho biết có hay không */
    public record ProfileView(String gender, LocalDate birthDate, String status, String phone,
                              String birthplace, String nationality, String address,
                              String cccdMasked, boolean hasEthnicity, boolean hasReligion) {
    }

    /** Giá trị đầy đủ của trường nhạy cảm, chỉ trả về khi admin yêu cầu rõ ràng */
    public record SensitiveView(String cccd, String ethnicity, String religion) {
    }

    private record Raw(String gender, LocalDate birthDate, String status, String phone,
                       String birthplace, String nationality, String address,
                       String cccdEnc, String cccdIndex, String ethnicityEnc, String religionEnc) {
    }

    private final JdbcTemplate jdbc;
    private final CccdCipher cipher;

    @Transactional(readOnly = true)
    public ProfileView find(Long userId) {
        final Raw raw = loadRaw(userId);
        return new ProfileView(
                raw.gender(),
                raw.birthDate(),
                raw.status(),
                raw.phone(),
                raw.birthplace(),
                raw.nationality(),
                raw.address(),
                raw.cccdEnc() == null ? null : CccdCipher.mask(cipher.decrypt(raw.cccdEnc())),
                raw.ethnicityEnc() != null,
                raw.religionEnc() != null);
    }

    /** Giải mã đầy đủ các trường nhạy cảm. Controller phải kiểm tra quyền và ghi nhật ký. */
    @Transactional(readOnly = true)
    public SensitiveView reveal(Long userId) {
        final Raw raw = loadRaw(userId);
        return new SensitiveView(
                decryptOrNull(raw.cccdEnc()),
                decryptOrNull(raw.ethnicityEnc()),
                decryptOrNull(raw.religionEnc()));
    }

    /**
     * Lưu hồ sơ. role là vai trò toàn trường của người dùng (ROLE_STUDENT, ROLE_LECTURER, ROLE_ADMIN)
     * để kiểm tra tình trạng hợp lệ. status null nghĩa là giữ tình trạng cũ.
     */
    @Transactional
    public void save(Long userId, String role, ProfileInput input) {
        final Raw raw = loadRaw(userId);

        final String gender = blankToNull(input.gender());
        if (gender != null && !GENDERS.contains(gender)) {
            throw new UnacceptableRequestDataException("Giới tính không hợp lệ");
        }

        final String status = blankToNull(input.status());
        if (status != null && !STATUSES_BY_ROLE.getOrDefault(role, Set.of()).contains(status)) {
            throw new UnacceptableRequestDataException("Tình trạng không phù hợp với vai trò của người dùng này");
        }

        String phone = blankToNull(input.phone());
        if (phone != null) {
            phone = phone.replaceAll("\\s", "");
            if (!PHONE.matcher(phone).matches()) {
                throw new UnacceptableRequestDataException("Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0 hoặc +84");
            }
        }

        final String birthplace = text(input.birthplace(), MAX_LONG_TEXT, "Nơi sinh");
        final String address = text(input.address(), MAX_LONG_TEXT, "Địa chỉ");
        final String nationalityInput = text(input.nationality(), MAX_SHORT_TEXT, "Quốc tịch");
        final String nationality = nationalityInput == null ? DEFAULT_NATIONALITY : nationalityInput;

        String cccdEnc = raw.cccdEnc();
        String cccdIndex = raw.cccdIndex();
        if (input.cccd() != null) {
            final String cccd = blankToNull(input.cccd());
            if (cccd == null) {
                cccdEnc = null;
                cccdIndex = null;
            } else {
                if (!CccdCipher.isValid(cccd)) {
                    throw new UnacceptableRequestDataException("CCCD phải gồm đúng 12 chữ số");
                }
                final String index = cipher.index(cccd);
                final Integer clash = jdbc.queryForObject("""
                                select (select count(*) from user_profiles where cccd_index = ? and user_id <> ?)
                                     + (select count(*) from student_profiles where cccd_index = ? and user_id <> ?)
                                """,
                        Integer.class, index, userId, index, userId);
                if (clash != null && clash > 0) {
                    throw new UnacceptableRequestDataException("CCCD này đã được gán cho người dùng khác");
                }
                cccdEnc = cipher.encrypt(cccd);
                cccdIndex = index;
            }
        }

        String ethnicityEnc = raw.ethnicityEnc();
        if (input.ethnicity() != null) {
            final String ethnicity = text(input.ethnicity(), MAX_SHORT_TEXT, "Dân tộc");
            ethnicityEnc = ethnicity == null ? null : cipher.encrypt(ethnicity);
        }

        String religionEnc = raw.religionEnc();
        if (input.religion() != null) {
            final String religion = text(input.religion(), MAX_SHORT_TEXT, "Tôn giáo");
            religionEnc = religion == null ? null : cipher.encrypt(religion);
        }

        jdbc.update("""
                        insert into user_profiles (user_id, gender, birth_date, status, phone, birthplace, nationality,
                                                   address, cccd_enc, cccd_index, ethnicity_enc, religion_enc, updated_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                        on conflict (user_id) do update set
                            gender = excluded.gender,
                            birth_date = excluded.birth_date,
                            status = excluded.status,
                            phone = excluded.phone,
                            birthplace = excluded.birthplace,
                            nationality = excluded.nationality,
                            address = excluded.address,
                            cccd_enc = excluded.cccd_enc,
                            cccd_index = excluded.cccd_index,
                            ethnicity_enc = excluded.ethnicity_enc,
                            religion_enc = excluded.religion_enc,
                            updated_at = now()
                        """,
                userId,
                gender,
                input.birthDate(),
                status == null ? raw.status() : status,
                phone,
                birthplace,
                nationality,
                address,
                cccdEnc,
                cccdIndex,
                ethnicityEnc,
                religionEnc);
    }

    /** Đọc hồ sơ thô. CCCD và chỉ mục lấy từ user_profiles, nếu chưa có thì lấy từ student_profiles. */
    private Raw loadRaw(Long userId) {
        return jdbc.query("""
                        select up.gender, up.birth_date, up.status, up.phone, up.birthplace, up.nationality, up.address,
                               coalesce(up.cccd_enc, sp.cccd_enc) as cccd_enc,
                               coalesce(up.cccd_index, sp.cccd_index) as cccd_index,
                               up.ethnicity_enc, up.religion_enc
                        from users u
                        left join user_profiles up on up.user_id = u.id
                        left join student_profiles sp on sp.user_id = u.id
                        where u.id = ?
                        """,
                        (rs, n) -> new Raw(
                                rs.getString("gender"),
                                rs.getObject("birth_date", LocalDate.class),
                                rs.getString("status"),
                                rs.getString("phone"),
                                rs.getString("birthplace"),
                                rs.getString("nationality"),
                                rs.getString("address"),
                                rs.getString("cccd_enc"),
                                rs.getString("cccd_index"),
                                rs.getString("ethnicity_enc"),
                                rs.getString("religion_enc")),
                        userId)
                .stream()
                .findFirst()
                .orElse(new Raw(null, null, null, null, null, null, null, null, null, null, null));
    }

    private String decryptOrNull(String encrypted) {
        return encrypted == null ? null : cipher.decrypt(encrypted);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String text(String value, int maxLength, String fieldName) {
        final String trimmed = blankToNull(value);
        if (trimmed != null && trimmed.length() > maxLength) {
            throw new UnacceptableRequestDataException(fieldName + " tối đa " + maxLength + " ký tự");
        }
        return trimmed;
    }
}