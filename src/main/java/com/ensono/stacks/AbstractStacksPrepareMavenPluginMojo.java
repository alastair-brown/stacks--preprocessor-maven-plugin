package com.ensono.stacks;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;

public abstract class AbstractStacksPrepareMavenPluginMojo extends AbstractMojo {
    static final String TEST_PATH = "/src/test/java";
    static final String JAVA_PATH = "/src/main/java";

    static final String APP_MODULE = "/app";
    static final String RESOURCES_PATH = "/src/main/resources";

    static final String APPLICATION_PROPERTIES = "application.yml";

    static final String PRE_PROCESSOR_OUTPUT_DIR = "/com";
    static final String JAVA_FILE = ".java";
    static final String TEST_FILE = "Test.java";

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(property = "projectLocation")
    String projectLocation;

    @Parameter(property = "buildPom", defaultValue = "true")
    boolean buildPom;

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
}
