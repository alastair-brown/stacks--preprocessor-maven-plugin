package com.ensono.stacks.projectconfig;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

import static com.ensono.stacks.utils.FileUtils.makePath;

public class ProjectConfigUtils {

    public static List<Path> filterPackageList(List<Path> allFiles, List<PathMatcher> coreMatchers) {

        return allFiles
                .stream()
                .filter(p ->
                        includePackage(p, coreMatchers)
                ).toList();
    }

    public static List<PathMatcher> buildMatcherList(List<String> profiles, ProjectConfig projectConfig) {
        List<PathMatcher> matcherList = new ArrayList<>();
        List<String> fullPackageList = buildPackageListFromConfig(profiles, projectConfig);
        fullPackageList.forEach(p -> {
            matcherList.add(FileSystems.getDefault().getPathMatcher("glob:" + p));
        });
        return matcherList;
    }

    public static List<String> buildPackageListFromConfig(List<String> profiles, ProjectConfig projectConfig) {
        List<String> fullPackageList = new ArrayList<>(projectConfig.getCoreIncludes());
        projectConfig.getProfileFilters().forEach(p -> {
                    if (profiles.contains(p.getId())) {
                        fullPackageList.addAll(p.getIncludes());
                    }
                }
        );
        return fullPackageList;
    }

    public static List<Path> buildPropertiesListFromConfig(List<String> profiles, Path sourceResourcesDir, ProjectConfig projectConfig) throws IOException {
        List<Path> fullPropertiesList = new ArrayList<>();
        fullPropertiesList.add(makePath(sourceResourcesDir, projectConfig.getCorePropertiesFile()));
        projectConfig.getProfileFilters().forEach(p -> {
            if (profiles.contains(p.getId())) {
                p.getPropertiesFile().forEach(pf -> {

                    try {
                        fullPropertiesList.add(makePath(sourceResourcesDir, pf));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                });
            }
        });
        return fullPropertiesList;
    }

    private static boolean includePackage(Path path, List<PathMatcher> matcherList) {
        for (PathMatcher pm :matcherList) {
            if (pm.matches(path)) {
                return true;
            }
        }
        return false;

    }
}
