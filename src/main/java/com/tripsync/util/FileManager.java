
package com.tripsync.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FileManager {

    private final Path storageDirectory;

    public FileManager() {
        storageDirectory = Paths.get("uploads")
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException e) {
            throw new RuntimeException(
                "Unable to create file storage directory.", e
            );
        }
    }

    // ==========================================
    // 1. UPLOAD / COPY A FILE
    // ==========================================

    public String uploadFile(String sourcePath) throws IOException {

        if (sourcePath == null || sourcePath.isBlank()) {
            throw new IllegalArgumentException(
                "Source path cannot be empty."
            );
        }

        Path source = Paths.get(sourcePath).toAbsolutePath().normalize();

        if (!Files.exists(source) || !Files.isRegularFile(source)) {
            throw new IllegalArgumentException(
                "Source file does not exist."
            );
        }

        String fileName = source.getFileName().toString();

        Path destination = getSafePath(fileName);

        Files.copy(
            source,
            destination,
            StandardCopyOption.REPLACE_EXISTING
        );

        System.out.println("File uploaded: " + fileName);

        return fileName;
    }

    // ==========================================
    // 2. READ A TEXT FILE
    // ==========================================

    public String readTextFile(String fileName) throws IOException {

        Path file = getSafePath(fileName);

        if (!Files.exists(file)) {
            throw new IllegalArgumentException(
                "File not found."
            );
        }

        return Files.readString(file);
    }

    // ==========================================
    // 3. LIST ALL STORED FILES
    // ==========================================

    public List<String> listFiles() throws IOException {

        List<String> fileNames = new ArrayList<>();

        try (Stream<Path> files = Files.list(storageDirectory)) {

            files.filter(Files::isRegularFile)
                 .forEach(file ->
                     fileNames.add(file.getFileName().toString())
                 );
        }

        return fileNames;
    }

    // ==========================================
    // 4. DELETE A FILE
    // ==========================================

    public void deleteFile(String fileName) throws IOException {

        Path file = getSafePath(fileName);

        if (!Files.exists(file)) {
            throw new IllegalArgumentException(
                "File not found."
            );
        }

        Files.delete(file);

        System.out.println("File deleted: " + fileName);
    }

    // ==========================================
    // 5. CHECK WHETHER A FILE EXISTS
    // ==========================================

    public boolean fileExists(String fileName) {

        return Files.exists(getSafePath(fileName));
    }

    // ==========================================
    // 6. GET STORAGE DIRECTORY
    // ==========================================

    public String getStorageDirectory() {
        return storageDirectory.toString();
    }

    // ==========================================
    // 7. VALIDATE FILE PATH
    // ==========================================

    private Path getSafePath(String fileName) {

        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException(
                "File name cannot be empty."
            );
        }

        Path file = Paths.get(fileName);

        if (file.isAbsolute() || file.getNameCount() != 1
                || fileName.equals(".") || fileName.equals("..")) {
            throw new IllegalArgumentException(
                "Invalid file name."
            );
        }

        Path resolved = storageDirectory.resolve(file).normalize();

        if (!resolved.startsWith(storageDirectory)) {
            throw new IllegalArgumentException(
                "Invalid file location."
            );
        }

        return resolved;
    }
}
