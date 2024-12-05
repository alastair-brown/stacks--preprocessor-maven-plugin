package com.ensono.stacks.utils.pom;

import java.util.List;
public record TemplateDependencyModel(
        String groupId,
        String artifactId,
        String version,
        List<TemplateExclusionModel> exclusions,
        boolean hasExclusions
) {}
