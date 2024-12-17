package com.ensono.stacks;

import com.ensono.stacks.projectconfig.ProjectConfig;
import com.ensono.stacks.projectconfig.ProjectConfigUtils;
import com.ensono.stacks.utils.ApplicationPropertiesFileBuilder;
import com.ensono.stacks.utils.XmlUtils;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Profile;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.ensono.stacks.projectconfig.ProjectConfigUtils.buildPropertiesListFromConfig;
import static com.ensono.stacks.projectconfig.ProjectConfigUtils.filterPackageList;
import static com.ensono.stacks.utils.FileUtils.*;


@Mojo(name = "stacks-prepare-project", defaultPhase = LifecyclePhase.COMPILE)
public class StacksPrepareSourceMavenPluginMojo extends AbstractStacksPrepareMavenPluginMojo {

    private List<String> activeProfileIds = new ArrayList<>();

    private List<PathMatcher> pathMatchers = new ArrayList<>();

    @Override
    public void execute() {

        getLog().info("Project output location - " + projectLocation);

        getLog().info("Working directory - " + Paths.get("").toAbsolutePath());

        // Parse the project config json file
        buildProjectConfig();

        // find list of active profiles set for this Maven project
        buildActiveProfiles();

        // Build the list of Path matchers based on the includes sections from the Project Config
        buildMatcherList();

        // Move any files that match with the matchers we've configured
        moveFiles();

        // generate the contents of the resources folder
        generateResources();

        // if we are building a POM do that.
        if (buildPom) {
            try {
                writePom();
            } catch (IOException e) {
                getLog().error("Error writing the Pom file", e);
                throw new RuntimeException(e);
            }
        } else {
            getLog().info("Configured to not generate Pom file");
        }
    }

    private void buildActiveProfiles() {
        activeProfileIds = getActiveProfiles(project)
                .stream()
                .map(Profile::getId)
                .toList();

        getLog().info("Profiles -" + project.getActiveProfiles());
    }

    private void moveFiles() {

        List<Path> allFiles = new ArrayList<>();
        FileVisitor<Path> fv = new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {

                if (path.toFile().isFile() && path.toString().endsWith(JAVA_FILE)) {
                    getLog().info("Adding file to move - " + path);
                    allFiles.add(path);
                }
                return FileVisitResult.CONTINUE;
            }
        };

        try {
            Path p = Paths.get(projectLocation);
            Files.walkFileTree(p, fv);
        } catch (IOException ioe) {
            getLog().error("Error walking tree", ioe);
        }

        List<Path> filteredFiles = filterPackageList(allFiles, pathMatchers);

        for (Path path : filteredFiles) {
            try {

                getLog().info("Source file to move - " + path);
                if (path.toFile().exists()) {
                    moveFile(path, buildJavaPath(path));
                }

            } catch (IOException ioe) {
                getLog().error("Error moving file - " + path, ioe);
            }
        }
        deleteDirectoryStructure(Path.of(projectLocation + PRE_PROCESSOR_OUTPUT_DIR));
    }

    private void buildMatcherList() {
        pathMatchers.addAll(ProjectConfigUtils.buildMatcherList(activeProfileIds, projectConfig));
    }

    private void generateResources() {
        try {
            Path sourceResourcesDir = makePath(Paths.get("").toAbsolutePath(),  RESOURCES_PATH);
            Path destinationResourcesDir = makePath(Path.of(projectLocation), RESOURCES_PATH);

            getLog().info("Using Resources directory - " + sourceResourcesDir);
            Path destinationApplicationProperties = makePath(destinationResourcesDir, projectConfig.getOutputPropertiesFile());

            List<Path> resources = new ArrayList<>(
                    buildPropertiesListFromConfig(activeProfileIds, sourceResourcesDir, projectConfig)
            );

            resources.forEach(r ->
                    getLog().info("Combining Props file - " + r)
            );

            ApplicationPropertiesFileBuilder.combineResourceFiles(resources, destinationApplicationProperties);

            if (projectConfig.hasAdditionalProperties()) {
                projectConfig.additionalProperties.forEach(additionalProp -> {
                    try {
                        Path sourceAdditionalProperties = makePath(sourceResourcesDir, additionalProp);
                        getLog().info("Copying additional properties file - " + sourceAdditionalProperties);
                        Path destAdditionalProperties = makePath(destinationResourcesDir, additionalProp);
                        copyFile(sourceAdditionalProperties, destAdditionalProperties);
                    } catch (IOException e) {
                        getLog().error("Error copying additional properties ", e);
                    }
                });
            }

        } catch (IOException e) {
            getLog().error("Error creating application properties ", e);
        }
    }

    private void writePom() throws IOException {

        File pomFile = new File(projectLocation + "/pom.xml");

        // gets the actual pom file from the stacks-java-preprocessor project
        String currentPom;
        Map<String, String> versionProperties = new HashMap<>();
        try {
            Path pomPath = makePath(Paths.get("").toAbsolutePath(), "pom.xml");

            currentPom = new String(Files.readAllBytes(pomPath));
            versionProperties = extractVersionProperties(currentPom, activeProfileIds);

            getLog().info("versionProperties = " + versionProperties);
        } catch (Exception e) {
            getLog().error("Error writing the Pom file", e);
            throw new RuntimeException(e);
        }

        MustacheFactory mf = new DefaultMustacheFactory() {
            // extract this method, this is used when the  mf.compile("/pom"); is triggered
            @Override
            public Reader getReader(String resourceName) {
                try {
                    getLog().info("RESOURCE =  " + resourceName);
                    // will need to filter the resource name depending on what is selected i.e. (AWS, AZURE, COSOMOS, etc)
                    // resourceName comes from the pom.mustache template itself i.e. ({{> core/coreManagedDependencies}})
                    Path path = Paths.get(makePath(Paths.get("").toAbsolutePath(), pomTemplateFile) + resourceName + ".mustache");
                    return new InputStreamReader(Files.newInputStream(path));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        Mustache mainTemplate = mf.compile("/pom");
        // Add the active profiles to the data model so that the profile specific templates are included.
        Map<String, Object> dataModel = new HashMap<>();
        addProfilesToDataModel(dataModel, activeProfileIds);

        // Render the template to a string
        StringWriter writer = new StringWriter();
        mainTemplate.execute(writer, dataModel).flush();
        String renderedTemplate = writer.toString();

        // Check if any keys from the map are present in the rendered template and construct the property strings
        List<String> propertiesList = new ArrayList<>();
        for (Map.Entry<String, String> entry : versionProperties.entrySet()) {
            String key = entry.getKey();
            getLog().info("versionPropertiesKey = " + key);
            String value = entry.getValue();
            getLog().info("versionPropertiesKey = " + value);
            if (renderedTemplate.contains(key)) {
                String propertyString = String.format("<%s>%s</%s>", key, value, key);
                propertiesList.add(propertyString);
            }
        }

        getLog().info("propertiesList = " + propertiesList);

        // Prepare data model
        dataModel.put("versionProperties", propertiesList);

        // Render the template with the property versions
        writer = new StringWriter();
        mainTemplate.execute(writer, dataModel).flush();

        try(FileWriter fileWriter = new FileWriter(pomFile)) {
            fileWriter.write(writer.toString());
            getLog().info("Pom file generated = " + pomFile);
        } catch (IOException e) {
            getLog().error("Unable to write POM file", e);
        }
    }

    private static Map<String, String> extractVersionProperties(String pomContent, List<String> activeProfileIds) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(pomContent.getBytes()));

        NodeList propertiesNodes = document.getElementsByTagName("properties");
        // if there are no properties then return an empty map
        if (propertiesNodes.getLength() == 0) {
            return Map.of();
        }

        Element propertiesElement = (Element) propertiesNodes.item(0);
        NodeList propertyNodes = propertiesElement.getChildNodes();

        Map<String, String> versionProperties = buildPropertiesMap(propertyNodes);

        // extract the versions properties from each off the active profiles and add them to the list of
        // properties to include.
        activeProfileIds.stream().forEach(id -> {
            System.out.println("Checking properties for = " + id);
            versionProperties.putAll(extractVersionPropertiesFromProfile(document, id));
        });

        return versionProperties;
    }

    private static Map<String, String> extractVersionPropertiesFromProfile(Document document, String profileName) {
        NodeList profiles = document.getElementsByTagName("profile");
        for (Node profile : XmlUtils.iterable(profiles)) {

            Element profileElement = (Element) profile;

            Node id = profileElement.getElementsByTagName("id").item(0);
            if (id.getTextContent().equals(profileName)) {
                Node profileProps = profileElement.getElementsByTagName("properties").item(0);
                if (profileProps != null) {
                    return buildPropertiesMap(profileProps.getChildNodes());
                }
            }
        }
        return Map.of();
    }

    private static Map<String, String> buildPropertiesMap(NodeList propertyNodes) {
        Map<String, String> versionProperties = new HashMap<>();
        IntStream.range(0, propertyNodes.getLength())
                .mapToObj(propertyNodes::item)
                .filter(node -> node instanceof Element)
                .map(node -> (Element) node)
                .filter(element -> element.getTagName().endsWith(".version"))
                .forEach(element -> versionProperties.put(element.getTagName(), element.getTextContent()));

        return versionProperties;
    }

//    private void writePom() throws IOException {
//        File pomFile = new File(projectLocation + "/pom.xml");
//        getLog().info("Generating Pom file = " + pomFile);
//
//        // Get the dependencies associated to the project
//        List<Dependency> parentDependencies = getDependencies(project.getParent());
//        List<Dependency> dependencies = getDependencies(project);
//        List<Dependency> managedDependencies = project.getDependencyManagement().getDependencies();
//
//        // Prepare data model for template using PomTemplateBuilder
//        PomTemplateBuilder builder = new PomTemplateBuilder();
//        Map<String, Object> dataModel = builder
//                .withDependencies(managedDependencies, parentDependencies, dependencies, projectConfig)
//                .build();
//
//        MustacheFactory mf = new DefaultMustacheFactory();
//        Path templatePath = makePath(Paths.get("").toAbsolutePath(), pomTemplateFile);
//        Mustache mustache;
//
//        try(InputStreamReader reader = new InputStreamReader(Files.newInputStream(templatePath))) {
//            mustache = mf.compile(reader, "template");
//        }
//
//        StringWriter writer = new StringWriter();
//
//        // Render the filtered dependencies into the actual POM file
//        mustache.execute(writer, dataModel).flush();
//
//        try(FileWriter fileWriter = new FileWriter(pomFile)) {
//            fileWriter.write(writer.toString());
//            getLog().info("Pom file generated = " + pomFile);
//        } catch (IOException e) {
//            getLog().error("Unable to write POM file", e);
//        }
//    }

    @SuppressWarnings("unchecked")
    public List<Dependency> getDependencies(MavenProject project) {
        return (List<Dependency>) project.getDependencies();
    }

    @SuppressWarnings("unchecked")
    public List<Profile> getActiveProfiles(MavenProject project) {
        return (List<Profile>) project.getActiveProfiles();
    }

    private static void addProfilesToDataModel(Map<String, Object> dataModel, List<String> activeProfileIds) {
        activeProfileIds.stream().forEach(id -> {
            dataModel.put(id, true);
        });
    }

}
