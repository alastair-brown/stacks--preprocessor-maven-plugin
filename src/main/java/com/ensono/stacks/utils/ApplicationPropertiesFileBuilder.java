package com.ensono.stacks.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class ApplicationPropertiesFileBuilder {

    public static void combineResourceFiles(List<Path> inputFiles, Path outputfile) throws IOException {
        List<Path> validFiles = inputFiles.stream()
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().toLowerCase().endsWith(".yml"))
                .toList();

        Files.deleteIfExists(outputfile);
        Path targetDir = outputfile.getParent();

        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }
        Files.createFile(outputfile);

        for (Path inputFile : validFiles) {
            List<String> lines = Files.readAllLines(inputFile);
            lines.add("\n");
            Files.write(outputfile, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        }
    }
}
