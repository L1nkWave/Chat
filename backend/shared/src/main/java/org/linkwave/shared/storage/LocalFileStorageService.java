package org.linkwave.shared.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;

public class LocalFileStorageService implements FileStorageService {

    public static final int MAX_FILENAME_LENGTH = 32;
    public static final String DEFAULT_FILENAME = "f";

    @Value("${files.storage-folder}")
    private String storageRootFolder;

    @Override
    public String storePicture(@NonNull Path rootDirPath,
                               @NonNull String folderName,
                               @NonNull MultipartFile file) throws IOException {
        final String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image")) {
            throw new IllegalArgumentException("Received file is not an image");
        }
        return storeFile(rootDirPath, folderName, file);
    }

    @Override
    public String storeFile(@NonNull Path rootDirPath,
                            @NonNull String folderName,
                            @NonNull MultipartFile file) throws IOException {

        // create chat folder if it does not exist
        Path folder = convertToStoragePath(rootDirPath.toString(), folderName);
        if (!folder.toFile().exists()) {
            folder = Files.createDirectories(folder);
        }

        final String filename = format("%s-%s", currentTimeMillis(), resolveFilename(file));

        // save file to chat folder
        Files.write(Path.of(folder.toString(), filename), file.getBytes());
        return filename;
    }

    private String resolveFilename(@NonNull MultipartFile file) {

        final String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.length() > MAX_FILENAME_LENGTH) {

            final String contentType = file.getContentType();

            if (contentType != null) { // then save file format
                final String fileFormat = contentType.substring(contentType.indexOf("/"));
                return format("%s.%s", DEFAULT_FILENAME, fileFormat);
            } else {
                return DEFAULT_FILENAME;
            }
        }
        return originalFilename;
    }

    @Override
    public byte[] readFileAsBytes(@NonNull Path path) throws IOException {
        return Files.readAllBytes(convertToStoragePath(path.toString()));
    }

    @NonNull
    private Path convertToStoragePath(String... folders) {
        return Path.of(storageRootFolder, folders);
    }

}
