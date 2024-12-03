package com.ensono.stacks.projectconfig;

import lombok.Data;

import java.util.List;

@Data
public class ProjectConfig {

    private List<String> coreIncludes;
    private List<ProfileFilter> profileFilters;
    private String corePropertiesFile;
    private String outputPropertiesFile;

}
