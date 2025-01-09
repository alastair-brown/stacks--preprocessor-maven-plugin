package com.ensono.stacks;

import com.ensono.stacks.projectconfig.ProjectConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.ensono.stacks.utils.FileUtils.makePath;

public abstract class AbstractStacksPrepareMavenPluginMojo extends AbstractMojo {
    static final String TEST_PATH = "/src/test/java";
    static final String JAVA_PATH = "/src/main/java";

    static final String APP_MODULE = "/app";
    static final String RESOURCES_PATH = "/src/main/resources";

    static final String PRE_PROCESSOR_OUTPUT_DIR = "/com";
    static final String JAVA_FILE = ".java";
    static final String TEST_FILE = "Test.java";

    ProjectConfig projectConfig;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(property = "projectLocation", required = true)
    String projectLocation;

    @Parameter(property = "projectConfigFile", required = true)
    String projectConfigFile;

    @Parameter(property = "pomTemplateFile", required = true)
    String pomTemplateFile;

    @Parameter(property = "buildPom", defaultValue = "true")
    boolean buildPom;

    ObjectMapper objectMapper = new ObjectMapper();

    Path buildTestPath(Path source) {
        String packagePath = StringUtils.replaceOnce(source.toString(), projectLocation, "");
        return Path.of(projectLocation + TEST_PATH + packagePath);
    }

    Path buildJavaPath(Path path) {
        String packagePath = StringUtils.replaceOnce(path.toString(), projectLocation, "");
        if (path.startsWith(JAVA_PATH)) {
            return path;
        } else {
            return Path.of(projectLocation + JAVA_PATH + packagePath);
        }
    }

    protected void buildProjectConfig() {
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
}
