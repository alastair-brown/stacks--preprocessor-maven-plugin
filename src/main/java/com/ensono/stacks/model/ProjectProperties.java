package com.ensono.stacks.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record ProjectProperties(
        @JacksonXmlProperty(localName = "maven.compiler.source") String mavenCompilerSource,
        @JacksonXmlProperty(localName = "maven.compiler.target") String mavenCompilerTarget
) {
    public ProjectProperties(String mavenCompilerSource, String mavenCompilerTarget) {
        this.mavenCompilerSource = mavenCompilerSource;
        this.mavenCompilerTarget = mavenCompilerTarget;
    }
}
