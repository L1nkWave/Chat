package org.linkwave.shared.storage;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

/**
 * Decides what a file really is from its content (magic bytes), not from the
 * Content-Type header or the file name supplied by the client.
 * <p>
 * Only an explicit allow-list is accepted. Anything else (HTML, SVG, JS, XML,
 * executables, unknown binaries) is rejected.
 */
public final class FileSignatures {

    public record Detected(String mimeType, String extension) {
    }

    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";

    private static final int TEXT_CHECK_LIMIT = 8192;
    private static final int MAX_NAME_LENGTH = 120;

    private FileSignatures() {
    }

    /**
     * @param content          full file content
     * @param originalFilename name sent by the client, used only to pick between formats that share a signature
     * @return the detected type, or empty if the file is not on the allow-list
     */
    public static Optional<Detected> detect(byte[] content, String originalFilename) {
        if (content == null || content.length == 0) {
            return Optional.empty();
        }

        if (matchesBytes(content, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return Optional.of(new Detected("image/png", "png"));
        }
        if (matchesBytes(content, 0, 0xFF, 0xD8, 0xFF)) {
            return Optional.of(new Detected("image/jpeg", "jpg"));
        }
        if (matchesAscii(content, 0, "GIF87a") || matchesAscii(content, 0, "GIF89a")) {
            return Optional.of(new Detected("image/gif", "gif"));
        }
        if (matchesAscii(content, 0, "RIFF") && matchesAscii(content, 8, "WEBP")) {
            return Optional.of(new Detected("image/webp", "webp"));
        }
        if (matchesAscii(content, 0, "%PDF-")) {
            return Optional.of(new Detected("application/pdf", "pdf"));
        }

        final String extension = extensionOf(originalFilename);

        // OOXML documents are ZIP containers; the extension selects the format
        if (matchesBytes(content, 0, 0x50, 0x4B, 0x03, 0x04)) {
            return switch (extension) {
                case "docx" -> Optional.of(new Detected(DOCX, "docx"));
                case "xlsx" -> Optional.of(new Detected(XLSX, "xlsx"));
                case "pptx" -> Optional.of(new Detected(PPTX, "pptx"));
                default -> Optional.empty();
            };
        }

        if ("txt".equals(extension) && isUtf8Text(content)) {
            return Optional.of(new Detected("text/plain", "txt"));
        }

        return Optional.empty();
    }

    /**
     * Images are the only types that may be shown inline. Everything else is served as an attachment.
     */
    public static boolean isInlineSafe(String mimeType) {
        return mimeType != null && (
                "image/png".equals(mimeType) ||
                        "image/jpeg".equals(mimeType) ||
                        "image/gif".equals(mimeType) ||
                        "image/webp".equals(mimeType)
        );
    }

    public static boolean isImage(Detected detected) {
        return detected != null && detected.mimeType().startsWith("image/");
    }

    /**
     * Keeps only the last path segment and removes control and reserved characters.
     * The result is used for display and for the Content-Disposition header, never for the storage path.
     */
    public static String sanitizeName(String name) {
        if (name == null) {
            return "file";
        }
        String base = name.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        base = base.replaceAll("[\\p{Cntrl}\"<>|:*?]", "_").trim();

        if (base.isEmpty() || ".".equals(base) || "..".equals(base)) {
            return "file";
        }
        return base.length() > MAX_NAME_LENGTH
                ? base.substring(base.length() - MAX_NAME_LENGTH)
                : base;
    }

    private static String extensionOf(String name) {
        if (name == null) {
            return "";
        }
        final int idx = name.lastIndexOf('.');
        return idx == -1 ? "" : name.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean matchesBytes(byte[] content, int offset, int... expected) {
        if (content.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((content[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesAscii(byte[] content, int offset, String expected) {
        return matchesBytes(content, offset, expected.chars().toArray());
    }

    private static boolean isUtf8Text(byte[] content) {
        final int limit = Math.min(content.length, TEXT_CHECK_LIMIT);
        for (int i = 0; i < limit; i++) {
            if (content[i] == 0) {
                return false;
            }
        }
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

}