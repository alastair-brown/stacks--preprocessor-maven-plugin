package com.ensono.stacks.projectconfig;

import com.ensono.stacks.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.ensono.stacks.utils.FileUtils.makePath;

public class ProjectConfigUtils {

    public static List<Path> filterPackageList(List<Path> allFiles, List<String> profiles, ProjectConfig projectConfig) {
        List<String> packagesToInclude = buildPackageListFromConfig(profiles, projectConfig);

        return allFiles
                .stream()
                .filter(p ->
                        includePackage(p, packagesToInclude)
                ).toList();
    }

    public static List<String> buildPackageListFromConfig(List<String> profiles, ProjectConfig projectConfig) {
        List<String> fullPackageList = new ArrayList<>(projectConfig.getCoreIncludes());
        projectConfig.getProfileFilters().forEach(p -> {
                    if (profiles.contains(p.getId())) {
                        fullPackageList.addAll(p.getPackages());
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

    private static boolean includePackage(Path path, List<String> packagesToInclude) {
        for (String packageToInclude : packagesToInclude) {

            if (FileUtils.containsSubPath(path, packageToInclude)) {
                return true;
            }
        }
        return false;
    }
}
