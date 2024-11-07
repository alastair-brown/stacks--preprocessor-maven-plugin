package com.ensono.stacks.model.build;

import com.ensono.stacks.model.ProjectDependency;

import java.util.List;

public class PluginFactory {

    private static final String SPRING_PLUGIN_GROUP_ID = "org.springframework.boot";

    private static final String SPRING_PLUGIN_ARTIFACT_ID = "spring-boot-maven-plugin";

    private static final String LOMBOK_GROUP_ID = "org.projectlombok";

    private static final String LOMBOK_ARTIFACT_ID = "lombok";

    public static SpringBootPlugin buildSpringBootPlugin() {
        return new SpringBootPlugin(
                SPRING_PLUGIN_GROUP_ID,
                SPRING_PLUGIN_ARTIFACT_ID,
                ProjectPluginConfiguration
                        .builder()
                        .excludes(
                                List.of(
                                        new ProjectDependency(LOMBOK_GROUP_ID, LOMBOK_ARTIFACT_ID)
                                )
                        )
                        .build()
        );
    }

}
