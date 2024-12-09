package com.ensono.stacks.projectconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;

public class ProjectConfigTest {

    private static ObjectMapper objectMapper;

    @BeforeAll
    public static void setObjectMapper() {
        objectMapper = new ObjectMapper();
    }

    @Test
    public void canParseProjectConfigJsonFile() throws IOException, URISyntaxException {

        File configFile = Paths.get(getClass().getResource("/project-builder-config.json").toURI()).toFile();
        ProjectConfig projectConfig = objectMapper.readValue(configFile, ProjectConfig.class);
        Assert.assertEquals("application.yml", projectConfig.getOutputPropertiesFile());
        Assert.assertEquals("application.yml", projectConfig.getCorePropertiesFile());
        assertProjectConfig(projectConfig);
    }

    @Test
    public void canParseCutdownProjectConfigJsonFile() throws IOException, URISyntaxException {

        File configFile = Paths.get(getClass().getResource("/cutdown-config.json").toURI()).toFile();
        ProjectConfig projectConfig = objectMapper.readValue(configFile, ProjectConfig.class);
        assertProjectConfig(projectConfig);

    }

    @Test
    public void canParseProjectConfigJsonFileWithNoPackageAttribute() throws IOException, URISyntaxException {

        File configFile = Paths.get(getClass().getResource("/project-builder-config_no_package.json").toURI()).toFile();
        ProjectConfig projectConfig = objectMapper.readValue(configFile, ProjectConfig.class);
        assertProjectConfig(projectConfig);

    }

    @Test
    public void canParseProjectConfigJsonFileWithNoProperties() throws IOException, URISyntaxException {

        File configFile = Paths.get(getClass().getResource("/project-builder-config_no_properties.json").toURI()).toFile();
        ProjectConfig projectConfig = objectMapper.readValue(configFile, ProjectConfig.class);
        assertProjectConfig(projectConfig);

    }

    public void assertProjectConfig(ProjectConfig projectConfig) {
        Assert.assertNotNull(projectConfig);
        Assert.assertEquals("application.yml", projectConfig.getOutputPropertiesFile());
        Assert.assertEquals("application.yml", projectConfig.getCorePropertiesFile());
        Assert.assertEquals(
                Arrays.asList(
                        "AI-Agent.xml",
                        "logback-spring.xml",
                        "auth.properties"
                ),
                projectConfig.additionalProperties
        );
        Assert.assertNotNull(projectConfig.getExcludedGroupIds());
    }
}
