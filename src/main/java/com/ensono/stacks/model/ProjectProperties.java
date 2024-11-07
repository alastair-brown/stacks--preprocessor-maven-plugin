package com.ensono.stacks.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;

@Getter
public class ProjectProperties {

    @JacksonXmlProperty(localName = "maven.compiler.source")
    private final String mavenCompilerSource;

    @JacksonXmlProperty(localName = "maven.compiler.target")
    private final String mavenCompilerTarget;

    public ProjectProperties(String mavenCompilerSource, String mavenCompilerTarget) {
        this.mavenCompilerSource = mavenCompilerSource;
        this.mavenCompilerTarget = mavenCompilerTarget;
    }
}
