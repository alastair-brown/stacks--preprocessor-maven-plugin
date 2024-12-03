package com.ensono.stacks;

import com.ensono.stacks.projectconfig.ProjectConfig;
import com.ensono.stacks.utils.ApplicationPropertiesFileBuilder;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Profile;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.ensono.stacks.projectconfig.ProjectConfigUtils.buildPropertiesListFromConfig;
import static com.ensono.stacks.projectconfig.ProjectConfigUtils.filterPackageList;
import static com.ensono.stacks.utils.FileUtils.*;


@Mojo(name = "stacks-prepare-project", defaultPhase = LifecyclePhase.COMPILE)
public class StacksPrepareSourceMavenPluginMojo extends AbstractStacksPrepareMavenPluginMojo {

    private List<String> activeProfileIds = new ArrayList<>();
    private static final List<String> REMOVABLE_DEPENDENCIES = new ArrayList<>(Arrays.asList(
            "systems.manifold",
            "com.github.spullara.mustache.java",
            "com.spotify.fmt"
    ));


    ProjectConfig projectConfig;

    @Override
    public void execute() {

        getLog().info("Project output location - " + projectLocation);

        getLog().info("Working directory - " + Paths.get("").toAbsolutePath());

        buildProjectConfig();

        buildActiveProfiles();

        moveFiles();

        generateResources();

        if (buildPom) {
            try {
                writePom(project.getDependencies());
            } catch (IOException e) {
                getLog().error("Unable to write POM file", e);
                throw new RuntimeException(e);
            }
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
                getLog().error("projectConfigFile property must be set");
            }
            Path projectConfigFile = makePath(Paths.get("").toAbsolutePath(), this.projectConfigFile);
            getLog().info("Reading project config from " +projectConfigFile);

            projectConfig = objectMapper.readValue(projectConfigFile.toFile(), ProjectConfig.class);
        } catch (IOException e) {
            getLog().error("Error reading projectConfigFile property");
        }

    }

    private void moveFiles() {

        List<Path> allFiles = new ArrayList<>();
        FileVisitor<Path> fv = new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {

                if (path.toFile().isFile() && path.toString().endsWith(JAVA_FILE)) {
                    getLog().info("Adding file to move - " + path);
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

        List<Path> filteredFiles = filterPackageList(allFiles, activeProfileIds, projectConfig);
        for (Path path : filteredFiles) {
            try {

                getLog().info("Source file to move - " + path);
                if (path.toFile().exists()) {
                    moveFile(path, buildJavaPath(path));
                }

            } catch (IOException ioe) {
                getLog().error("Error moving file - " + path, ioe);
            }
        }
        deleteDirectoryStructure(Path.of(projectLocation + PRE_PROCESSOR_OUTPUT_DIR));
    }

    private void generateResources() {
        try {
            List<Path> resources = new ArrayList<>();
            Path sourceResourcesDir = makePath(Paths.get("").toAbsolutePath(), APP_MODULE + RESOURCES_PATH);
            Path destinationResourcesDir = makePath(Path.of(projectLocation), RESOURCES_PATH);

            getLog().info("Using Resources directory - " + sourceResourcesDir);
            Path destinationApplicationProperties = makePath(destinationResourcesDir, projectConfig.getOutputPropertiesFile());

            resources.addAll(buildPropertiesListFromConfig(activeProfileIds, sourceResourcesDir, projectConfig));

            resources.forEach(r ->
                    getLog().info("Combining Props file - " + r)
            );

            ApplicationPropertiesFileBuilder.combineResourceFiles(resources, destinationApplicationProperties);
        } catch (IOException e) {
            getLog().error("Error creating application properties ", e);
        }
    }

    private void writePom(List<Dependency> dependencies) throws IOException {
        File pomFile = new File(projectLocation + "/pom.xml");
        getLog().info("Generating Pom file = " + pomFile);

        // Filter dependencies based on removable dependencies
        List<Dependency> filteredDependencies = dependencies.stream()
                .filter(dep -> !REMOVABLE_DEPENDENCIES.contains(dep.getGroupId()))
                .toList();

        MustacheFactory mf = new DefaultMustacheFactory();
        Path templatePath = makePath(Paths.get("").toAbsolutePath(),"app/src/main/resources/templates/template.mustache");
        Mustache mustache;

        try(InputStreamReader reader = new InputStreamReader(Files.newInputStream(templatePath))) {
            mustache = mf.compile(reader, "template");
        }

        StringWriter writer = new StringWriter();

        // add filtered dependencies to pom
        mustache.execute(writer, Collections.singletonMap("dependencies", filteredDependencies)).flush();

        try(FileWriter fileWriter = new FileWriter(pomFile)) {
            fileWriter.write(writer.toString());
            getLog().info("Pom file generated = " + pomFile);
        } catch (IOException e) {
            getLog().error("Unable to write POM file", e);
        }

    }
}
