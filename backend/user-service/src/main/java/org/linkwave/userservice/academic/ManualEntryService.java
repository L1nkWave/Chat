package org.linkwave.userservice.academic;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.linkwave.userservice.academic.AcademicImportService.ImportReport;
import org.linkwave.userservice.academic.AcademicImportService.Kind;
import org.linkwave.userservice.exception.UnacceptableRequestDataException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thêm một dòng bằng form. Form được đổi thành một file Excel một dòng rồi đi qua đúng luồng import,
 * nên mọi quy tắc kiểm tra, trùng lặp và thứ tự nhập giống hệt khi nhập file.
 */
@Service
@RequiredArgsConstructor
public class ManualEntryService {

    /** Tên cột đúng như file import yêu cầu. Khóa của form trùng với tên cột này. */
    private static final Map<Kind, List<String>> HEADERS = Map.of(
            Kind.LECTURERS, List.of("Mã GV", "Họ tên"),
            Kind.STUDENTS, List.of("MSSV", "Họ tên", "Lớp", "CCCD"),
            Kind.TIMETABLE, List.of("Mã MH", "Môn", "Nhóm", "Phòng", "Thứ", "Tiết", "Số tiết",
                    "GV", "Mã GV", "Lớp", "Ngày", "Giờ bắt đầu"),
            Kind.EXAMS, List.of("Mã MH", "Tên môn học", "Sĩ số", "Ngày thi", "Giờ bắt đầu", "Phòng thi", "Cơ sở"),
            Kind.DUTIES, List.of("Mã GV", "Mã MH", "Ngày", "Giờ thi", "Phòng", "Cơ sở"));

    private final AcademicImportService importService;

    public static boolean supports(Kind kind) {
        return HEADERS.containsKey(kind);
    }

    public static List<String> headersOf(Kind kind) {
        return HEADERS.getOrDefault(kind, List.of());
    }

    /** Thêm một dòng. Ném lỗi với thông báo đầu tiên nếu dòng không hợp lệ hoặc đã có. */
    public ImportReport add(Kind kind, Map<String, String> values) throws IOException {
        final List<String> headers = headersOf(kind);
        if (headers.isEmpty()) {
            throw new UnacceptableRequestDataException("Mục này không thêm bằng form");
        }
        final Map<String, String> row = new LinkedHashMap<>();
        for (String header : headers) {
            row.put(header, values.getOrDefault(header, "") == null ? "" : values.getOrDefault(header, "").trim());
        }

        final byte[] bytes = toWorkbook(headers, row);
        final ImportReport report;
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            report = importService.run(kind, in, false);
        }
        if (!report.errors().isEmpty()) {
            final var first = report.errors().get(0);
            throw new UnacceptableRequestDataException(first.message());
        }
        if (report.written() == 0) {
            throw new UnacceptableRequestDataException("Không ghi được dòng nào");
        }
        return report;
    }

    private static byte[] toWorkbook(List<String> headers, Map<String, String> row) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            final Sheet sheet = workbook.createSheet("Sheet1");
            final Row header = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                header.createCell(i).setCellValue(headers.get(i));
            }
            final Row data = sheet.createRow(1);
            for (int i = 0; i < headers.size(); i++) {
                // Ghi mọi ô dạng chữ để ngày, giờ, số tiết không bị đổi kiểu
                data.createCell(i).setCellValue(row.getOrDefault(headers.get(i), ""));
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }
}