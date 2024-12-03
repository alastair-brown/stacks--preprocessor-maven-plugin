package com.ensono.stacks.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.apache.maven.model.Dependency;

@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ProjectDependency {
    private final String groupId;
    private final String artifactId;
    private String version;
    private String type;
    private String scope;

    public ProjectDependency(Dependency dependency) {
        this.groupId = dependency.getGroupId();
        this.artifactId = dependency.getArtifactId();
        this.version = dependency.getVersion();

        if (!dependency.getType().equals("jar")) {
            this.type = dependency.getType();
        }

        if (!dependency.getScope().equals("compile")) {
            this.scope = dependency.getScope();
        }
    }

    public ProjectDependency(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public ProjectDependency(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

}
