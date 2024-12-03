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

public class ProjectConfigTest {

    private static ObjectMapper objectMapper;

    @BeforeAll
    public static void setObjectMapper() {
        objectMapper = new ObjectMapper();
    }
    @Test
    public void canParseProjectConfigJsonFile() throws IOException, URISyntaxException {

        File configFile =Paths.get(getClass().getResource("/project-builder-config.json").toURI()).toFile();
        ProjectConfig projectConfig = objectMapper.readValue(configFile, ProjectConfig.class);
        Assert.assertEquals("application.yml", projectConfig.getOutputPropertiesFile());
        Assert.assertEquals("application.yml", projectConfig.getCorePropertiesFile());
    }
}
