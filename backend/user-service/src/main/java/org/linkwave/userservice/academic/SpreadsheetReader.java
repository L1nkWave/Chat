package org.linkwave.userservice.academic;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.*;
import org.linkwave.userservice.exception.UnacceptableRequestDataException;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Đọc sheet đầu tiên của file .xlsx. Dòng đầu là tiêu đề.
 * Tên cột so khớp không phân biệt hoa thường, dấu tiếng Việt và khoảng trắng: "Mã MH" = "ma mh" = "MAMH".
 * Ô ngày được đổi về dd/MM/yyyy, ô giờ về HH:mm, còn lại lấy đúng chữ hiển thị trong Excel (giữ số 0 đầu).
 */
public final class SpreadsheetReader {

    public static final int MAX_ROWS = 5000;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    /** Các cột có trong file. Tên đã chuẩn hóa. */
    public record Table(Set<String> headers, List<RawRow> rows) {
    }

    /** Một dòng dữ liệu. number là số dòng trong Excel (bắt đầu từ 1), dùng cho thông báo lỗi. */
    public record RawRow(int number, Map<String, String> cells) {
        /** Giá trị của tên cột đầu tiên có dữ liệu. Cho phép nhiều tên cho cùng một trường (ví dụ "Sĩ số" hoặc "Số thí sinh"). */
        public String get(String... headers) {
            for (String header : headers) {
                final String value = cells.getOrDefault(normalize(header), "").trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
            return "";
        }
    }

    private SpreadsheetReader() {
    }

    public static Table read(InputStream in) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(in)) {
            final Sheet sheet = workbook.getSheetAt(0);
            final DataFormatter formatter = new DataFormatter(Locale.ROOT);
            final FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            final org.apache.poi.ss.usermodel.Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) {
                return new Table(Set.of(), List.of());
            }

            final Map<Integer, String> columns = new LinkedHashMap<>();
            for (int c = header.getFirstCellNum(); c >= 0 && c < header.getLastCellNum(); c++) {
                final String name = normalize(formatter.formatCellValue(header.getCell(c)));
                if (!name.isEmpty()) {
                    columns.putIfAbsent(c, name);
                }
            }

            final List<RawRow> rows = new ArrayList<>();
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                final org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                final Map<String, String> cells = new HashMap<>();
                boolean blank = true;
                for (Map.Entry<Integer, String> column : columns.entrySet()) {
                    final String value = cellText(row.getCell(column.getKey()), formatter, evaluator);
                    if (!value.isEmpty()) {
                        blank = false;
                    }
                    cells.put(column.getValue(), value);
                }
                if (blank) {
                    continue;
                }
                if (rows.size() >= MAX_ROWS) {
                    throw new UnacceptableRequestDataException("File có quá " + MAX_ROWS + " dòng dữ liệu");
                }
                rows.add(new RawRow(r + 1, cells));
            }
            return new Table(new HashSet<>(columns.values()), rows);
        } catch (EncryptedDocumentException | IllegalArgumentException e) {
            throw new UnacceptableRequestDataException("Không đọc được file. Hãy dùng file .xlsx đúng mẫu.");
        }
    }

    /** Chuẩn hóa tên cột/tiêu đề để so khớp: bỏ dấu, bỏ khoảng trắng và ký tự đặc biệt, chữ thường */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        final String decomposed = Normalizer.normalize(text.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static String cellText(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            final LocalDateTime when = cell.getLocalDateTimeCellValue();
            // Ô chỉ có giờ thì số nhỏ hơn 1 (phần ngày bằng 0)
            return cell.getNumericCellValue() < 1.0 ? when.format(TIME) : when.toLocalDate().format(DATE);
        }
        return formatter.formatCellValue(cell, evaluator).trim();
    }
}