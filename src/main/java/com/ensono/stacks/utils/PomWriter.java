package com.ensono.stacks.utils;

import com.ensono.stacks.utils.XmlUtils;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class PomWriter {
    private final String projectLocation;
    private final String pomTemplateFile;
    private final List<String> activeProfileIds;

    public PomWriter(String projectLocation, String pomTemplateFile, List<String> activeProfileIds) {
        this.projectLocation = projectLocation;
        this.pomTemplateFile = pomTemplateFile;
        this.activeProfileIds = activeProfileIds;
    }

    public void writePom() throws IOException {
        File pomFile = new File(projectLocation + "/pom.xml");

        // gets the actual pom file from the stacks-java-preprocessor project
        String currentPom;
        Map<String, String> versionProperties;
        Path pomPath = makePath(Paths.get("").toAbsolutePath(), "pom.xml");
        try {
            currentPom = new String(Files.readAllBytes(pomPath));
            versionProperties = extractVersionProperties(currentPom, activeProfileIds);

        } catch (Exception e) {
            String errorMessage = String.format("Error processing POM file at '%s'", pomPath);
            throw new RuntimeException(errorMessage, e);
        }

        Mustache mainTemplate = getMainTemplate();
        Map<String, Object> dataModel = new HashMap<>();
        addProfilesToDataModel(dataModel, activeProfileIds);

        // Render the template to a string
        StringWriter writer = new StringWriter();
        mainTemplate.execute(writer, dataModel).flush();
        String renderedTemplate = writer.toString();

        // Check if any keys from the map are present in the rendered template and construct the property strings
        List<String> propertiesList = getPropertiesList(versionProperties, renderedTemplate);

        // Prepare data model
        dataModel.put("versionProperties", propertiesList);

        // Render the template with the property versions
        writer = new StringWriter();
        mainTemplate.execute(writer, dataModel).flush();

        try (FileWriter fileWriter = new FileWriter(pomFile)) {
            fileWriter.write(writer.toString());
        } catch (IOException e) {
            String errorMessage = String.format("Error writing to POM file at '%s'", pomFile.getAbsolutePath());
            throw new RuntimeException(errorMessage, e);
        }
    }

    private Mustache getMainTemplate() {
        MustacheFactory mf = new DefaultMustacheFactory() {
            @Override
            public Reader getReader(String resourceName) {
                try {
                    // will need to filter the resource name depending on what is selected i.e. (AWS, AZURE, COSOMOS, etc)
                    // resourceName comes from the pom.mustache template itself i.e. ({{> core/coreManagedDependencies}})
                    Path path = Paths.get(makePath(Paths.get("")
                            .toAbsolutePath(), pomTemplateFile) + resourceName + ".mustache");
                    return new InputStreamReader(Files.newInputStream(path));
                } catch (IOException e) {
                    String errorMessage = String.format(
                            "Error reading resource '%s' from path '%s'", resourceName, pomTemplateFile);
                    throw new RuntimeException(errorMessage, e);
                }
            }
        };

        return mf.compile("/pom");
    }

    private static List<String> getPropertiesList(Map<String, String> versionProperties, String renderedTemplate) {
        List<String> propertiesList = new ArrayList<>();
        for (Map.Entry<String, String> entry : versionProperties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (renderedTemplate.contains(key)) {
                String propertyString = String.format("<%s>%s</%s>", key, value, key);
                propertiesList.add(propertyString);
            }
        }
        return propertiesList;
    }

    private Map<String, String> extractVersionProperties(
            String pomContent, List<String> activeProfileIds
    ) throws Exception {
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
        activeProfileIds.forEach(id -> {
            versionProperties.putAll(extractVersionPropertiesFromProfile(document, id));
        });

        return versionProperties;
    }

    private Map<String, String> extractVersionPropertiesFromProfile(Document document, String profileName) {
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

    private Map<String, String> buildPropertiesMap(NodeList propertyNodes) {
        Map<String, String> versionProperties = new HashMap<>();
        IntStream.range(0, propertyNodes.getLength())
                .mapToObj(propertyNodes::item)
                .filter(node -> node instanceof Element)
                .map(node -> (Element) node)
                .filter(element -> element.getTagName().endsWith(".version"))
                .forEach(element -> versionProperties.put(element.getTagName(), element.getTextContent()));

        return versionProperties;
    }

    private Path makePath(Path basePath, String pomTemplateFile) {
        return basePath.resolve(pomTemplateFile);
    }

    private static void addProfilesToDataModel(Map<String, Object> dataModel, List<String> activeProfileIds) {
        activeProfileIds.forEach(id -> {
            dataModel.put(id, true);
        });
    }
}