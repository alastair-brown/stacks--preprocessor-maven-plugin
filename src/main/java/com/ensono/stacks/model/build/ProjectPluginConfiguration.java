package com.ensono.stacks.model.build;

import com.ensono.stacks.model.ProjectDependency;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

@Builder
public class ProjectPluginConfiguration {

    @JacksonXmlElementWrapper(localName = "excludes")
    @JacksonXmlProperty(localName = "exclude")
    private List<ProjectDependency> excludes;
}
