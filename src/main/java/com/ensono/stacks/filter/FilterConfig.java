package com.ensono.stacks.filter;

import com.ensono.stacks.utils.FileUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FilterConfig {

    private static List<String> corePackages = new ArrayList<>();
    private static Map<String, FilterItem> profileFilter = new HashMap<>();

    static {
        profileFilter.put("aws", new FilterItem("com/ensono/stacks/stacks_preprocessor/aws", "application-aws.yml"));
        profileFilter.put("azure", new FilterItem("com/ensono/stacks/stacks_preprocessor/azure","application-azure.yml"));
        profileFilter.put("cosmos", new FilterItem("com/ensono.stacks/stacks_preprocessor/cosmos","application-cosmos.yml"));
        profileFilter.put("dynamodb", new FilterItem("com/ensono/stacks/stacks_preprocessor/dynamodb","application-dynamodb.yml"));
        profileFilter.put("kafka", new FilterItem("com/ensono/stacks.stacks_preprocessor/kafka"));
        profileFilter.put("servicebus", new FilterItem("com/ensono/stacks/stacks_preprocessor/servicebus"));
        profileFilter.put("sqs", new FilterItem("com/ensono/stacks.stacks_preprocessor/sqs"));

        corePackages.add("com/ensono/stacks/stacks_preprocessor/controller");
        corePackages.add("com/ensono/stacks/stacks_preprocessor/commons");
        corePackages.add("com/ensono/stacks/stacks_preprocessor/service");
        corePackages.add("com/ensono/stacks/stacks_preprocessor/repository");
        corePackages.add("com/ensono/stacks/stacks_preprocessor/StacksPreprocessorApplication.java");
    }

    public static Map<String, FilterItem> getProfileFilter() {
        return profileFilter;
    }

    public static List<Path> filterPackageList(List<Path> allFiles, List<String> profiles) {
        List<String> packagesToInclude = buildPackageList(profiles);

        return allFiles
                .stream()
                .filter(p ->
                    includePackage(p, packagesToInclude)
                ).toList();
    }

    public static List<String> buildPackageList(List<String> profiles) {
        List<String> fullPackageList = new ArrayList<>();
        fullPackageList.addAll(corePackages);
        profileFilter.keySet().forEach( key -> {
            if (profiles.contains(key)) {
                fullPackageList.add(profileFilter.get(key).getPackageName());
            }
        });

        return fullPackageList;
    }

    private static boolean includePackage(Path path, List<String> packagesToInclude) {
        for (String packageToInclude: packagesToInclude) {

            if (FileUtils.containsSubPath(path, packageToInclude)) {
                return true;
            }
        }

        return false;
    }
}
