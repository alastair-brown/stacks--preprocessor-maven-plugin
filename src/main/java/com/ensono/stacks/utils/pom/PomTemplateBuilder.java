package com.ensono.stacks.utils.pom;

import com.ensono.stacks.projectconfig.ProjectConfig;
import org.apache.maven.model.Dependency;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PomTemplateBuilder {
    private List<Dependency> managedDependencies;
    private List<Dependency> dependencies;
    private List<Dependency> parentDependencies;
    private ProjectConfig projectConfig;

    public PomTemplateBuilder withDependencies(
            List<Dependency> managedDependencies,
            List<Dependency> parentDependencies,
            List<Dependency> dependencies,
            ProjectConfig projectConfig) {
        this.managedDependencies = managedDependencies;
        this.parentDependencies = parentDependencies;
        this.dependencies = dependencies;
        this.projectConfig = projectConfig;
        return this;
    }

    public Map<String, Object> build() {
        List<Dependency> filteredDependencies = buildDependencyList();
        List<String> versionProperties = buildVersionProperties(filteredDependencies);

        List<TemplateDependencyModel> templateDependencies = filteredDependencies.stream()
                .map(dep -> new TemplateDependencyModel(
                        dep.getGroupId(),
                        dep.getArtifactId(),
                        isManagedDependency(dep) ? null : "${" + getVersionProperty(dep,versionProperties) + "}",
                        dep.getExclusions() == null ? List.of() : dep.getExclusions().stream()
                                .map(exclusion -> new TemplateExclusionModel(
                                        exclusion.getGroupId(),
                                        exclusion.getArtifactId()))
                                .collect(Collectors.toList()),
                        dep.getExclusions() != null && !dep.getExclusions().isEmpty()))
                .collect(Collectors.toList());

        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("dependencies", templateDependencies);
        dataModel.put("versionProperties", versionProperties);

        return dataModel;
    }

    private String getVersionProperty(Dependency dep, List<String> versionProperties) {
        String target = dep.getGroupId() + dep.getArtifactId() + ".version";
        return versionProperties.stream()
                .filter(prop -> prop.equals(target))
                .findFirst()
                .orElse(null);
    }

    private List<Dependency> buildDependencyList() {
        return dependencies.stream()
                .filter(dep -> parentDependencies.stream()
                        .noneMatch(parentDep -> parentDep.getGroupId().equals(dep.getGroupId()) &&
                                parentDep.getArtifactId().equals(dep.getArtifactId()) &&
                                parentDep.getVersion().equals(dep.getVersion())) &&
                        !projectConfig.getExcludedGroupIds().contains(dep.getGroupId()))
                .toList();
    }

    private List<String> buildVersionProperties(List<Dependency> filteredDependencies) {
        return filteredDependencies.stream()
                .filter(dep -> managedDependencies.stream()
                        .noneMatch(parentDep -> parentDep.getGroupId().equals(dep.getGroupId()) &&
                                parentDep.getArtifactId().equals(dep.getArtifactId()) &&
                                parentDep.getVersion().equals(dep.getVersion())))
                .map(this::buildPropertyString)
                .distinct()
                .collect(Collectors.toList());
    }

    private boolean isManagedDependency(Dependency dep) {
        return managedDependencies.stream()
                .anyMatch(parentDep -> parentDep.getGroupId().equals(dep.getGroupId()) &&
                        parentDep.getArtifactId().equals(dep.getArtifactId()) &&
                        parentDep.getVersion().equals(dep.getVersion()));
    }

    private String buildPropertyString(Dependency dep) {
        return String.format("<%s>%s</%s>", dep.getArtifactId() + ".version", dep.getVersion(), dep.getArtifactId() + ".version");
    }
}