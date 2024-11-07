package com.ensono.stacks;

import com.ensono.stacks.utils.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "stacks-prepare-project-tests", defaultPhase = LifecyclePhase.TEST_COMPILE)
public class StacksPrepareTestsMavenPluginMojo extends AbstractStacksPrepareMavenPluginMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("ProjectLocation - " + projectLocation);

        moveFiles();
    }

    private void moveFiles() {

        List<Path> allFiles = new ArrayList<>();
        FileVisitor<Path> fv = new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                    throws IOException {
                System.out.println("File = " + path);

                if (path.toFile().isFile() && path.toString().endsWith(JAVA_FILE)) {
                    allFiles.add(path);
                }
                return FileVisitResult.CONTINUE;
            }

        };

        try {
            Path p = Paths.get(projectLocation);
            Files.walkFileTree(p, fv);
        } catch (IOException ioe) {
            getLog().error("Error walking tree", ioe);
        }

        for (Path path : allFiles) {
            try {

                if (path.toFile().exists()) {
                    if (path.toString().endsWith(TEST_FILE)) {
                        getLog().info("Test file to move = " + path);
                        FileUtils.moveFile(path, buildTestPath(path));
                    }
                }

            } catch (IOException ioe) {
                getLog().error("Error moving file " + path, ioe);
            }
        }
        FileUtils.deleteDirectoryStructure(Path.of(projectLocation + PRE_PROCESSOR_OUTPUT_DIR));


    }


}
