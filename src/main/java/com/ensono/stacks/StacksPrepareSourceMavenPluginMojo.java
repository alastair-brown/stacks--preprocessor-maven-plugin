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
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ensono.stacks.projectconfig.ProjectConfigUtils.buildPropertiesListFromConfig;
import static com.ensono.stacks.projectconfig.ProjectConfigUtils.filterPackageList;
import static com.ensono.stacks.utils.FileUtils.deleteDirectoryStructure;
import static com.ensono.stacks.utils.FileUtils.makePath;
import static com.ensono.stacks.utils.FileUtils.moveFile;


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
                writePom(getDependencies(project));
            } catch (IOException e) {
                getLog().error("Unable to write POM file", e);
                throw new RuntimeException(e);
            }
        } else {
            getLog().info("Configured to not generate Pom file = ");
        }
    }

    private void buildActiveProfiles() {
        activeProfileIds = getActiveProfiles(project)
                .stream()
                .map(Profile::getId)
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
            Path sourceResourcesDir = makePath(Paths.get("").toAbsolutePath(), APP_MODULE + RESOURCES_PATH);
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
        } catch (IOException e) {
            getLog().error("Error creating application properties ", e);
        }
    }

    private void writePom(List<Dependency> dependencies) throws IOException {
        File pomFile = new File(projectLocation + "/pom.xml");
        getLog().info("Generating Pom file = " + pomFile);

        // Prepare data model for Mustache
        Map<String, Object> dataModel = createMustacheDataModel(dependencies);

        MustacheFactory mf = new DefaultMustacheFactory();
        Path templatePath = makePath(Paths.get("").toAbsolutePath(),"app/src/main/resources/templates/template.mustache");
        Mustache mustache;

        try(InputStreamReader reader = new InputStreamReader(Files.newInputStream(templatePath))) {
            mustache = mf.compile(reader, "template");
        }

        StringWriter writer = new StringWriter();

        // add filtered dependencies to pom
        mustache.execute(writer, dataModel).flush();

        try(FileWriter fileWriter = new FileWriter(pomFile)) {
            fileWriter.write(writer.toString());
            getLog().info("Pom file generated = " + pomFile);
        } catch (IOException e) {
            getLog().error("Unable to write POM file", e);
        }

    }

    private Map<String, Object> createMustacheDataModel(List<Dependency> dependencies) {

        // Filter dependencies we know we don't need
        List<Dependency> filteredDependencies = dependencies.stream()
                .filter(dep -> !REMOVABLE_DEPENDENCIES.contains(dep.getGroupId()))
                .collect(Collectors.toList());

        // Extract GroupId and version into a list of strings
        List<String> versionProperties = filteredDependencies.stream()
                .map(dep -> {
                    String propertyName = dep.getGroupId() + ".version";
                    return String.format("<%s>%s</%s>", propertyName, dep.getVersion(), propertyName);
                })
                .distinct()
                .collect(Collectors.toList());

        // Update the dependencies to use the new property
        filteredDependencies.forEach(dep -> {
            String propertyName = dep.getGroupId() + ".version";
            dep.setVersion("${" + propertyName + "}");
        });

        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("dependencies", filteredDependencies);
        dataModel.put("versionProperties", versionProperties);

        return dataModel;
    }

    @SuppressWarnings("unchecked")
    public List<Dependency> getDependencies(MavenProject project) {
        return (List<Dependency>) project.getDependencies();
    }

    @SuppressWarnings("unchecked")
    public List<Profile> getActiveProfiles(MavenProject project) {
        return (List<Profile>) project.getActiveProfiles();
    }
}
