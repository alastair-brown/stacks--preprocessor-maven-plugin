package com.ensono.stacks.model.build;

import org.apache.maven.model.Plugin;

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

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public Object getConfiguration() {
        return configuration;
    }
}
