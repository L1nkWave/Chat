package org.linkwave.shared.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static java.lang.String.format;

public class LocalFileStorageService implements FileStorageService {

    @Value("${files.storage-folder}")
    private String storageRootFolder;

    @Override
    public String storePicture(@NonNull Path rootDirPath,
                               @NonNull String folderName,
                               @NonNull MultipartFile file) throws IOException {

        final FileSignatures.Detected detected = FileSignatures.detect(file.getBytes(), file.getOriginalFilename())
                .orElse(null);
        if (!FileSignatures.isImage(detected)) {
            throw new IllegalArgumentException("Received file is not an image");
        }
        return storeContent(rootDirPath, folderName, file.getBytes(), detected.extension());
    }

    @Override
    public String storeContent(@NonNull Path rootDirPath,
                               @NonNull String folderName,
                               @NonNull byte[] content,
                               @NonNull String extension) throws IOException {

        // create folder if it does not exist
        Path folder = convertToStoragePath(rootDirPath.toString(), folderName);
        if (!folder.toFile().exists()) {
            folder = Files.createDirectories(folder);
        }

        final String filename = format("%s.%s", UUID.randomUUID(), extension);

        // save file to folder
        Files.write(Path.of(folder.toString(), filename), content);
        return filename;
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