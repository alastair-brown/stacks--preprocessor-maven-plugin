package com.ensono.stacks.projectconfig;

import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
public class ProjectConfig {

    private List<String> coreIncludes;
    private List<ProfileFilter> profileFilters;
    private String corePropertiesFile;
    private String outputPropertiesFile;

    public Optional<ProfileFilter> getProfileFilter(String id) {
        return profileFilters.stream().filter(profileFilter -> id.equals(profileFilter.getId())).findFirst();
    }

}
