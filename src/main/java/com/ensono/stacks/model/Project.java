package com.ensono.stacks.model;

import com.ensono.stacks.model.build.ProjectBuild;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.List;

import static com.ensono.stacks.model.ProjectConstants.OUTPUT_GROUP_SUFFIX;

@JsonPropertyOrder({ "modelVersion", "parent", "groupId", "artifactId", "version", "properties", "dependencies", "build" })
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JacksonXmlRootElement(localName = "project")
public class Project {


    @JacksonXmlProperty(isAttribute = true)
    private final String xmlns = "http://maven.apache.org/POM/4.0.0";
    @JacksonXmlProperty(localName = "xmlns:xsi", isAttribute = true)
    private final String xmlnsXsi = "http://www.w3.org/2001/XMLSchema-instance";
    @JacksonXmlProperty(localName = "xsi:schemaLocation", isAttribute = true)
    private final String xsiSchemaLocation = "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd";

    private final String modelVersion;

    private final ProjectDependency parent;
    private final String groupId;
    private final String artifactId;

    private final String version;

    private ProjectBuild build;

    @JacksonXmlElementWrapper(localName = "dependencies")
    @JacksonXmlProperty(localName = "dependency")
    private final List<ProjectDependency> dependencies = new ArrayList<>();

    private final ProjectProperties properties;

    public Project(MavenProject project) {
        this.modelVersion = project.getModelVersion();

        this.parent = new ProjectDependency(
                project.getParent().getGroupId(),
                project.getParent().getArtifactId(),
                project.getParent().getVersion()
        );


        this.groupId = project.getGroupId();
        this.artifactId = project.getArtifactId() + OUTPUT_GROUP_SUFFIX;
        this.version = ProjectConstants.BASE_VERSION;
        this.properties = new ProjectProperties(ProjectConstants.JAVA_VERSION, ProjectConstants.JAVA_VERSION);
        project.getDependencies().forEach(d ->
                this.dependencies.add(new ProjectDependency((Dependency) d))
        );
        this.build = new ProjectBuild(project);
    }

    public String getXmlns() {
        return xmlns;
    }

    public String getXmlnsXsi() {
        return xmlnsXsi;
    }

    public String getXsiSchemaLocation() {
        return xsiSchemaLocation;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public ProjectDependency getParent() {
        return parent;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public ProjectProperties getProperties() {
        return properties;
    }

    public List<ProjectDependency> getDependencies() {
        return dependencies;
    }

    public ProjectBuild getBuild() {
        return build;
    }

    public void setBuild(ProjectBuild build) {
        this.build = build;
    }
}
