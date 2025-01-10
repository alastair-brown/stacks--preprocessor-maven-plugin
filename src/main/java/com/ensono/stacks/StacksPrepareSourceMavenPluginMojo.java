package com.ensono.stacks;

import com.ensono.stacks.projectconfig.ProjectConfig;
import com.ensono.stacks.projectconfig.ProjectConfigUtils;
import com.ensono.stacks.utils.ApplicationPropertiesFileBuilder;
import com.ensono.stacks.utils.PomWriter;
import org.apache.maven.model.Profile;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.PathMatcher;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static com.ensono.stacks.projectconfig.ProjectConfigUtils.buildPropertiesListFromConfig;
import static com.ensono.stacks.projectconfig.ProjectConfigUtils.filterPackageList;
import static com.ensono.stacks.utils.FileUtils.copyFile;
import static com.ensono.stacks.utils.FileUtils.deleteDirectoryStructure;
import static com.ensono.stacks.utils.FileUtils.makePath;
import static com.ensono.stacks.utils.FileUtils.moveFile;


@Mojo(name = "stacks-prepare-project", defaultPhase = LifecyclePhase.COMPILE)
public class StacksPrepareSourceMavenPluginMojo extends AbstractStacksPrepareMavenPluginMojo {

    private List<String> activeProfileIds = new ArrayList<>();

    private final List<PathMatcher> pathMatchers = new ArrayList<>();

    @Override
    public void execute() {

        getLog().info("Project output location - " + projectLocation);

        getLog().info("Working directory - " + Paths.get("").toAbsolutePath());

        // Parse the project config json file
        buildProjectConfig();

        // find list of active profiles set for this Maven project
        buildActiveProfiles();

        // Build the list of Path matchers based on the includes sections from the Project Config
        buildMatcherList();

        // Move any files that match with the matchers we've configured
        moveFiles();

        // generate the contents of the resources folder
        generateResources();

        // if we are building a POM do that.
        if (buildPom) {
            try {
                PomWriter pomWriter = new PomWriter(projectLocation, pomTemplateFile, activeProfileIds);
                pomWriter.writePom();
            } catch (IOException e) {
                getLog().error("Error writing the Pom file", e);
                throw new RuntimeException(e);
            }
        } else {
            getLog().info("Configured to not generate Pom file");
        }
    }

    private void buildActiveProfiles() {
        activeProfileIds = getActiveProfiles(project)
                .stream()
                .map(Profile::getId)
                .toList();

        getLog().info("Profiles -" + project.getActiveProfiles());
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

        List<Path> filteredFiles = filterPackageList(allFiles, pathMatchers);

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

    private void buildMatcherList() {
        pathMatchers.addAll(ProjectConfigUtils.buildMatcherList(activeProfileIds, projectConfig));
    }

    private void generateResources() {
        try {
            Path sourceResourcesDir = makePath(Paths.get("").toAbsolutePath(),  RESOURCES_PATH);
            Path destinationResourcesDir = makePath(Path.of(projectLocation), RESOURCES_PATH);

            getLog().info("Using Resources directory - " + sourceResourcesDir);
            Path destinationApplicationProperties = makePath(destinationResourcesDir, projectConfig.getOutputPropertiesFile());

            List<Path> resources = new ArrayList<>(
                    buildPropertiesListFromConfig(activeProfileIds, sourceResourcesDir, projectConfig)
            );

            resources.forEach(r ->
                    getLog().info("Combining Props file - " + r)
            );

            ApplicationPropertiesFileBuilder.combineResourceFiles(resources, destinationApplicationProperties);

            if (projectConfig.hasAdditionalProperties()) {
                projectConfig.additionalProperties.forEach(additionalProp -> {
                    try {
                        Path sourceAdditionalProperties = makePath(sourceResourcesDir, additionalProp);
                        getLog().info("Copying additional properties file - " + sourceAdditionalProperties);
                        Path destAdditionalProperties = makePath(destinationResourcesDir, additionalProp);
                        copyFile(sourceAdditionalProperties, destAdditionalProperties);
                    } catch (IOException e) {
                        getLog().error("Error copying additional properties ", e);
                    }
                });
            }

        } catch (IOException e) {
            getLog().error("Error creating application properties ", e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Profile> getActiveProfiles(MavenProject project) {
        return (List<Profile>) project.getActiveProfiles();
    }
}
