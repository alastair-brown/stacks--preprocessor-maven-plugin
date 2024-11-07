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
}
