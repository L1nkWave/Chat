package org.linkwave.shared.storage;

import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface FileStorageService {

    /**
     * Stores passed file into storage.
     *
     * @param rootDirPath the path that contains folders with stored files
     * @param folderName  the name of the folder you want to store file in
     * @param file        the file that is supposed to be stored in storage
     * @return generated filename for passed file
     * @throws IOException if the root dir not found or can't create folder in root dir
     * @see LocalFileStorageService
     */
    String storeFile(@NonNull Path rootDirPath,
                     @NonNull String folderName,
                     @NonNull MultipartFile file) throws IOException;

    /**
     *
     * Stores passed picture into storage. Acts almost identically as {@link  FileStorageService#storeFile}
     * but additionally checks if passed file is a picture.
     *
     * @return generated filename for passed file
     * @throws IOException if the root dir not found, can't create folder in root dir, or file is not an image
     */
    String storePicture(@NonNull Path rootDirPath,
                        @NonNull String folderName,
                        @NonNull MultipartFile file) throws IOException;

    byte[] readFileAsBytes(@NonNull Path path) throws IOException;

}
