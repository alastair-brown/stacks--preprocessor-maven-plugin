package com.ensono.stacks;

import com.ensono.stacks.filter.FilterConfig;
import com.ensono.stacks.filter.FilterItem;
import com.ensono.stacks.model.Project;
import com.ensono.stacks.model.build.PluginFactory;
import com.ensono.stacks.model.build.ProjectBuild;
import com.ensono.stacks.projectconfig.ProjectConfig;
import com.ensono.stacks.utils.ApplicationPropertiesFileBuilder;
import com.ensono.stacks.utils.FileUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import org.apache.maven.model.Profile;
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

    List<String> activeProfileIds = new ArrayList<>();

    ProjectConfig projectConfig;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("ProjectLocation - " + projectLocation);

        getLog().info("Working directory -" + Paths.get("").toAbsolutePath());

        buildProjectConfig();

        buildActiveProfiles();

        moveFiles();

        generateResources();

        if (buildPom) {
            writePom();
        } else {
            getLog().info("Configured to not generate Pom file = ");
        }
    }

    private void buildActiveProfiles() {
        activeProfileIds = (List<String>) project.getActiveProfiles()
                .stream()
                .map(
                        p -> ((Profile) p).getId()
                )
                .toList();

        getLog().info("Profiles -" + project.getActiveProfiles());
    }

    private void buildProjectConfig() {
        try {
            if (projectConfigFile == null) {
                getLog().error("projectConfigFile must be set");
            }
            Path projectConfigFile = FileUtils.makePath(Path.of(projectLocation), this.projectConfigFile);
            projectConfig = objectMapper.readValue(projectConfigFile.toFile(), ProjectConfig.class);
        } catch (IOException e) {
            getLog().error("Error reading projectConfigFile");
        }

        getLog().info(projectConfig.toString());

    }

    private void moveFiles() {

        List<Path> allFiles = new ArrayList<>();
        FileVisitor<Path> fv = new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                    throws IOException {

                if (path.toFile().isFile() && path.toString().endsWith(JAVA_FILE)) {
                    getLog().info("Adding file to move = " + path);
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

        List<Path> filteredFiles = FilterConfig.filterPackageList(allFiles, activeProfileIds);
        for (Path path : filteredFiles) {
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

    private void generateResources() {
        try {
            List<Path> resources = new ArrayList<>();
            Path sourceResourcesDir = FileUtils.makePath(Paths.get("").toAbsolutePath(), APP_MODULE + RESOURCES_PATH);
            Path destinationResourcesDir = FileUtils.makePath(Path.of(projectLocation), RESOURCES_PATH);

            getLog().info("Using Resources directory - " + sourceResourcesDir);
            Path sourceApplicationProperties = FileUtils.makePath(sourceResourcesDir, APPLICATION_PROPERTIES);
            Path destinationApplicationProperties = FileUtils.makePath(destinationResourcesDir, APPLICATION_PROPERTIES);

            resources.add(sourceApplicationProperties);

            activeProfileIds.forEach(id -> {
                FilterItem item = FilterConfig.getProfileFilter().get(id);
                if (item.hasProperties()) {
                    try {
                        Path sourceAdditionalApplicationProperties = FileUtils.makePath(sourceResourcesDir, item.getProperties());
                        resources.add(sourceAdditionalApplicationProperties);
                    } catch (IOException ioe) {
                        getLog().error("Error creating reference to properties file", ioe);
                    }
                }
            });

            resources.forEach(r ->
                    getLog().info("Combining Props file - " + r)
            );

            ApplicationPropertiesFileBuilder.combineResourceFiles(resources, destinationApplicationProperties);
        } catch (IOException e) {
            getLog().error("Error creating application properties ", e);
        }
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
