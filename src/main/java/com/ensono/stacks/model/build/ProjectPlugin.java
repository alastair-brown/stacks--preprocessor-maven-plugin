package com.ensono.stacks.model.build;

import lombok.Getter;
import org.apache.maven.model.Plugin;

@Getter
public class ProjectPlugin {

    private final String groupId;
    private final String artifactId;

    private final Object configuration;

    public ProjectPlugin(Plugin plugin) {
        this.groupId = plugin.getGroupId();
        this.artifactId = plugin.getArtifactId();
        this.configuration = plugin.getConfiguration();
    }


    public ProjectPlugin(String groupId, String artifactId, Object configuration) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.configuration = configuration;
    }
}
