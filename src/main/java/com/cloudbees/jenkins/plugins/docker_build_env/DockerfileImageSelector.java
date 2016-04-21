package com.cloudbees.jenkins.plugins.docker_build_env;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.TaskListener;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DockerfileImageSelector extends DockerImageSelector {

    private String contextPath;

    private String dockerfile;

    @DataBoundConstructor
    public DockerfileImageSelector(String contextPath, String dockerfile) {
        this.contextPath = contextPath;
        this.dockerfile = dockerfile;
    }

    @Override
    public String prepareDockerImage(Docker docker, AbstractBuild build, TaskListener listener, boolean forcePull) throws IOException, InterruptedException {

        String expandedContextPath = build.getEnvironment(listener).expand(getContextPath());
        FilePath dockerBuildWorkspace = build.getWorkspace().child(expandedContextPath);
        if ( !dockerBuildWorkspace.isDirectory() ) {
            listener.getLogger().println("Docker build path does not exist or is not a directory: " + expandedContextPath);
            throw new InterruptedException("Docker build path does not exist or is not a directory: " + expandedContextPath);
        }

        String expandedDockerfile = build.getEnvironment(listener).expand(getDockerfile());
        FilePath dockerFile = dockerBuildWorkspace.child(expandedDockerfile);
        if (!dockerFile.exists()) {
            listener.getLogger().println("Your Dockerfile is missing: " + expandedDockerfile);
            throw new InterruptedException("Your Dockerfile is missing: " + expandedDockerfile);
        }

        listener.getLogger().println("Build Docker image from $WORKSPACE/" + expandedContextPath + "/"+expandedDockerfile+" ...");
        return docker.buildImage(dockerBuildWorkspace, expandedDockerfile, forcePull);
    }

    @Override
    public Collection<String> getDockerImagesUsedByJob(Job<?, ?> job) {
        // TODO get last build and parse Dockerfile "FROM"
        return Collections.EMPTY_LIST;
    }

    public String getContextPath() {
        return contextPath;
    }

    public String getDockerfile() {
        return isEmpty(dockerfile) ? "Dockerfile" : dockerfile;
    }

    private Object readResolve() {
        if (dockerfile == null) dockerfile="Dockerfile";
        return this;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<DockerImageSelector> {

        @Override
        public String getDisplayName() {
            return "Build from Dockerfile";
        }
    }
}
