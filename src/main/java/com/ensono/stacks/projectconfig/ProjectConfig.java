package com.ensono.stacks.projectconfig;

import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
public class ProjectConfig {
    static final String APPLICATION_PROPERTIES = "application.yml";

    private List<String> coreIncludes;
    private List<ProfileFilter> profileFilters;
    private String corePropertiesFile = APPLICATION_PROPERTIES;
    private String outputPropertiesFile = APPLICATION_PROPERTIES;

    public Optional<ProfileFilter> getProfileFilter(String id) {
        return profileFilters.stream().filter(profileFilter -> id.equals(profileFilter.getId())).findFirst();
    }

}
