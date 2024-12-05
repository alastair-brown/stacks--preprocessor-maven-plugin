package com.ensono.stacks.projectconfig;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
public class ProjectConfig {
    static final String APPLICATION_PROPERTIES = "application.yml";

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> coreIncludes = new ArrayList<>();

    private List<ProfileFilter> profileFilters = new ArrayList<>();;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> excludedGroupIds = new ArrayList<>();;

    private String corePropertiesFile = APPLICATION_PROPERTIES;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    public List<String> additionalProperties = new ArrayList<>();;

    private String outputPropertiesFile = APPLICATION_PROPERTIES;

    public Optional<ProfileFilter> getProfileFilter(String id) {
        return profileFilters.stream().filter(profileFilter -> id.equals(profileFilter.getId())).findFirst();
    }

    public boolean hasAdditionalProperties() {
        return !this.additionalProperties.isEmpty();
    }

}
