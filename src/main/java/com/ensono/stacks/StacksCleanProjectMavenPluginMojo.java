package com.ensono.stacks;

import com.ensono.stacks.utils.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;

@Mojo(name = "stacks-clean-project", defaultPhase = LifecyclePhase.CLEAN)
public class StacksCleanProjectMavenPluginMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(property = "projectLocation")
    String projectLocation;


    @Override
    public void execute() {
        getLog().info("Do clean on " + projectLocation);
        FileUtils.deleteDirectoryStructure(Path.of(projectLocation));
    }
}
