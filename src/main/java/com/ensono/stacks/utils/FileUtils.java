package com.ensono.stacks.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

public class FileUtils {


    public static void deleteDirectoryStructure(Path dir) {
        if (Files.exists(dir) && Files.isDirectory(dir)) {
            try (var paths = Files.walk(dir)) {
                paths
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void moveFile(Path source, Path target) throws IOException {
        Path targetDir = target.getParent();

        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void copyFile(Path source, Path target) throws IOException {
        Path targetDir = target.getParent();

        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    public static boolean fileExists(Path fileToCheck) throws IOException {
        return Files.exists(fileToCheck);
    }

    public static boolean endsWithSeperator(Path pathToCheck) throws IOException {
        return pathToCheck.endsWith(pathToCheck.getFileSystem().getSeparator());
    }

    public static Path makePath(Path path, String pathPart) throws IOException {
        String sep = path.getFileSystem().getSeparator();
        if (endsWithSeperator(path)) {
            return Path.of(path + pathPart);
        }
        return Path.of(path +sep+ pathPart);

    }

    public static boolean containsSubPath(Path path, String subpath) {
        return path.toString().contains(subpath);
    }


}
