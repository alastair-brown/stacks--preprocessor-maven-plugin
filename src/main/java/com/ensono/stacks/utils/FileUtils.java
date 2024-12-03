package com.ensono.stacks.utils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

public class FileUtils {


    public static void deleteDirectoryStructure(Path dir) {
        if (Files.exists(dir) && Files.isDirectory(dir)) {
            try (var paths = Files.walk(dir)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public static void moveFile(Path source, Path target) throws IOException {
        ensureTargetDirectoryExists(target);
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void copyFile(Path source, Path target) throws IOException {
        ensureTargetDirectoryExists(target);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    private static void ensureTargetDirectoryExists(Path target) throws IOException {
        Path targetDir = target.getParent();
        if (targetDir != null && !Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }
    }

    public static boolean fileExists(Path fileToCheck) {
        return Files.exists(fileToCheck);
    }

    public static boolean endsWithSeparator(Path pathToCheck) {
        return pathToCheck.endsWith(pathToCheck.getFileSystem().getSeparator());
    }

    public static Path makePath(Path path, String pathPart) throws IOException {
        String sep = path.getFileSystem().getSeparator();
        if (endsWithSeparator(path)) {
            return Path.of(path + pathPart);
        }
        return Path.of(path + sep + pathPart);

    }

    public static boolean containsSubPath(Path path, String subPath) {
        return path.toString().contains(subPath);
    }
}
