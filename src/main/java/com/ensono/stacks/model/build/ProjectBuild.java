package com.ensono.stacks.model.build;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ProjectBuild {
    @JacksonXmlElementWrapper(localName = "plugins")
    @JacksonXmlProperty(localName = "plugin")
    private List<ProjectPlugin> plugins = new ArrayList<>();

    public ProjectBuild(MavenProject project) {
        project.getBuildPlugins().forEach(p ->
                plugins.add(new ProjectPlugin((Plugin) p))
        );
    }

    public ProjectBuild(List<ProjectPlugin> plugins) {
        this.plugins = plugins;
    }
}
