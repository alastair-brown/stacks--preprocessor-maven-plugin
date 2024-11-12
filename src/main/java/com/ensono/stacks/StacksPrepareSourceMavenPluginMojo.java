package com.ensono.stacks;

import com.ensono.stacks.model.Project;
import com.ensono.stacks.model.build.PluginFactory;
import com.ensono.stacks.model.build.ProjectBuild;
import com.ensono.stacks.utils.FileUtils;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "stacks-prepare-project", defaultPhase = LifecyclePhase.COMPILE)
public class StacksPrepareSourceMavenPluginMojo extends AbstractStacksPrepareMavenPluginMojo {

    private static final String TEST_PATH = "/src/test/java";
    private static final String JAVA_PATH = "/src/main/java";

    private static final String JAVA_FILE = ".java";



    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("ProjectLocation - " + projectLocation);

        moveFiles();

        writePom();
    }

    private void moveFiles() {

        List<Path> allFiles = new ArrayList<>();
        FileVisitor<Path> fv = new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                    throws IOException {

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

                    getLog().info("Source file to move = " + path);
                    if (path.toFile().exists()) {
                        FileUtils.moveFile(path, buildJavaPath(path));

                    }

                } catch (IOException ioe) {
                    getLog().error("Error moving file " + path, ioe);
                }
            }
            FileUtils.deleteDirectoryStructure(Path.of(projectLocation + PRE_PROCESSOR_OUTPUT_DIR));

    }

    private void writePom() {
        File pomFile = new File(projectLocation + "/pom.xml");
        getLog().info("Generating Pom file = " + pomFile);
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
        xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
        try {
            Project pomModel = new Project(project);
            pomModel.setBuild(new ProjectBuild(List.of(PluginFactory.buildSpringBootPlugin())));
            xmlMapper.writeValue(pomFile, pomModel);
        } catch (IOException e) {
            getLog().error("Unable to write POM file", e);
        }

    }
}
