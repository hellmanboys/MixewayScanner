package io.mixeway.scanner.utils;

import io.mixeway.scanner.rest.model.ScanRequest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author gsiewruk
 */
@Component
public class CodeHelper {
    public static String sourceLocation;

    @Value( "${sources.location}" )
    public void setSourceLocation(String sourcesPath) {
        sourceLocation = sourcesPath;
    }

    /**
     * Gettig repo name from URL
     * e.g: https://github.com/mixeway/mixewayhub.git should return mixewayhub
     *
     * @param repoUrl URL for repository
     * @return name of repository
     */
    public static String getNameFromRepoUrlforSAST(String repoUrl){
        String[] partsOfUrl = repoUrl.split("/");
        String repoName = partsOfUrl[partsOfUrl.length-1];
        return repoName.split("\\.")[0];
    }

    /**
     * Determine type of source code in given location - JAVA-MVN, JAVA-Gradle, NPM, PIP or PHP-COMPOSER
     *
     */
    public static SourceProjectType getSourceProjectTypeFromDirectory(ScanRequest scanRequest){
        String projectLocation = sourceLocation + File.separatorChar + getNameFromRepoUrlforSAST(scanRequest.getTarget());
        File pom = new File(projectLocation + File.separatorChar + "pom.xml");
        if(pom.exists()){
            return SourceProjectType.MAVEN;
        }
        File gradle = new File(projectLocation + File.separatorChar + "build.xml");
        if (gradle.exists()) {
            prepareGradle(gradle);
            return SourceProjectType.GRADLE;
        }
        File npm = new File(projectLocation + File.separatorChar + "packages.json");
        if(npm.exists()){
            return SourceProjectType.NPM;
        }
        File composer = new File(projectLocation + File.separatorChar + "composer.json");
        if(composer.exists()){
            return SourceProjectType.COMPOSER;
        }
        Collection pip = FileUtils.listFiles(
                new File(projectLocation),
                new RegexFileFilter(".*\\.py"),
                DirectoryFileFilter.DIRECTORY
        ).stream().map(File::getName).collect(Collectors.toList());
        if (pip.size() > 3) {
            return SourceProjectType.PIP;
        }
        return null;

    }

    /**
     * Method which preare build.xml file to use CycloneDX plugin to generate SBOM
     *
     * @param gradle path to gradle file
     */
    private static void prepareGradle(File gradle) {

        //TODO edit of build.xml
    }

    /**
     * Method which return path to project git downloaded
     *
     * @param scanRequest to process
     * @return path to code
     */
    public static String getProjectPath(ScanRequest scanRequest){
        return  sourceLocation + File.separatorChar + getNameFromRepoUrlforSAST(scanRequest.getTarget());
    }
}