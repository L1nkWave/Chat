package org.linkwave.shared.storage;

import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface FileStorageService {

    /**
     * Stores already validated content under a server-generated name.
     * The client's file name is never used on disk.
     *
     * @param rootDirPath the path that contains folders with stored files
     * @param folderName  the name of the folder you want to store file in
     * @param content     validated file content
     * @param extension   extension chosen by the server from the detected type, without a dot
     * @return generated filename
     * @throws IOException if the root dir not found or can't create folder in root dir
     * @see LocalFileStorageService
     */
    String storeContent(@NonNull Path rootDirPath,
                        @NonNull String folderName,
                        @NonNull byte[] content,
                        @NonNull String extension) throws IOException;

    /**
     * Stores passed picture into storage. The type is decided from the file content,
     * not from the Content-Type header.
     *
     * @return generated filename
     * @throws IOException              if the root dir not found or can't create folder in root dir
     * @throws IllegalArgumentException if the content is not a supported image
     */
    String storePicture(@NonNull Path rootDirPath,
                        @NonNull String folderName,
                        @NonNull MultipartFile file) throws IOException;

    byte[] readFileAsBytes(@NonNull Path path) throws IOException;

}