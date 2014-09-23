package org.checkerframework.mavenplugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * A set of utility methods to find the necessary JSR 308 jars and to resolve any sources/classes needed
 * for compilation and checking
 * @author Adam Warski (adam at warski dot org)
 */
public class PathUtils {
    private final static String CHECKER_FRAMEWORK_GROUPD_ID = "org.checkerframework";
    private final static String DEFAULT_INCLUSION_PATTERN = "**/*.java";

    /**
     * Gets the path to the jsr308-all jar.
     * @param checkerFrameworkVersion Version of the Checker Framework to use.
     * @param artifactFactory Dependency.
     * @param artifactResolver Dependency.
     * @param remoteArtifactRepositories Dependency.
     * @param localRepository Dependency.
     * @return Path to the jsr308-all jar.
     * @throws MojoExecutionException
     */
    public static File getFrameworkJar(final String artifactId, final String checkerFrameworkVersion, final ArtifactFactory artifactFactory,
                                         final ArtifactResolver artifactResolver, final List<?> remoteArtifactRepositories,
                                         final ArtifactRepository localRepository) throws MojoExecutionException {
        final Artifact checkersArtifact;
        try {
            checkersArtifact = artifactFactory.createExtensionArtifact(CHECKER_FRAMEWORK_GROUPD_ID, artifactId,
                    VersionRange.createFromVersionSpec(checkerFrameworkVersion));
        } catch (InvalidVersionSpecificationException e) {
            throw new MojoExecutionException("Wrong version: " + checkerFrameworkVersion + " of Checker Framework specified.", e);
        }

        try {
            artifactResolver.resolve(checkersArtifact, remoteArtifactRepositories, localRepository);
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException("Unable to find version " + checkerFrameworkVersion + " of Checker Framework.", e);
        } catch (ArtifactNotFoundException e) {
            throw new MojoExecutionException("Unable to resolve version " + checkerFrameworkVersion + " of Checker Framework.", e);
        }

        return checkersArtifact.getFile();
    }

    public static void writeVersion(final File versionFile, final String version) throws IOException {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(versionFile));
            bw.write(version);
            bw.newLine();
            bw.flush();

        } finally {
            if(bw != null) {
                bw.close();
            }
        }
    }

    public static String readVersion(final File versionFile) throws IOException {
        if( !versionFile.exists() || versionFile.length() == 0) {
            return null;
        }

        final BufferedReader vfReader = new BufferedReader(new FileReader(versionFile));

        String line;
        try {
            line = vfReader.readLine();
            if(line == null) {
                return null;
            }

        } finally {
            vfReader.close();
        }

        return line;
    }

    /**
     * Gets the path to the given executable, looking it up in the toolchain.
     * @param executable Name of the executable.
     * @param toolchainManager Dependency.
     * @param session Dependency.
     * @return Path to the executable.
     */
    public static String getExecutablePath(String executable, final ToolchainManager toolchainManager, final MavenSession session) {
        final File execFile = new File(executable);
        if (execFile.exists()) {
            return execFile.getAbsolutePath();
        } else {
            final Toolchain tc = toolchainManager.getToolchainFromBuildContext("jdk", session);

            if (tc != null) {
                executable = tc.findTool(executable);
            }
        }

        return executable;
    }

    /**
     * Scans the given compile source roots for sources, taking into account the given includes and excludes.
     * @param compileSourceRoots A list of source roots.
     * @param sourceIncludes Includes specification.  Defaults to DEFAULT_INCLUSION_PATTERN if no sourceIncludes are
     *                       specified
     * @param sourceExcludes Excludes specification.
     * @return A list of included sources from the given source roots.
     */
    public static List<String> scanForSources(final List<?> compileSourceRoots, final Set<String> sourceIncludes,
                                              final Set<String> sourceExcludes) {
        if (sourceIncludes.isEmpty()) {
            sourceIncludes.add(DEFAULT_INCLUSION_PATTERN);
        }

        final List<String> sources = new ArrayList<String>();

        for (Object compileSourceRoot : compileSourceRoots) {
            final File compileSourceRootFile = new File(compileSourceRoot.toString());
            final String[] sourcesFromSourceRoot =
                    scanForSources(compileSourceRootFile, sourceIncludes, sourceExcludes);

            for (final String sourceFromSourceRoot : sourcesFromSourceRoot) {
                sources.add(new File(compileSourceRootFile, sourceFromSourceRoot).getAbsolutePath());
            }
        }

        return sources;
    }

    /**
     * Scans a single source dir for sources and includes only the files whose name match the patterns in
     * sourceIncludes and excludes all files whose names match the patterns in sourceExcludes
     * @param sourceDir The directory to scan
     * @param sourceIncludes Only include a file if its name matches a pattern in sourceIncludes
     * @param sourceExcludes Exclude a file if its name matches a pattern in sourceExcludes
     * @return A set of filepath strings
     */
    private static String[] scanForSources(final File sourceDir, final Set<String> sourceIncludes,
                                           final Set<String> sourceExcludes) {
        final DirectoryScanner ds = new DirectoryScanner();
        ds.setFollowSymlinks( true );
        ds.setBasedir( sourceDir );

        ds.setIncludes(sourceIncludes.toArray(new String[sourceIncludes.size()]));
        ds.setExcludes(sourceExcludes.toArray(new String[sourceExcludes.size()]));

        ds.addDefaultExcludes();

        try {
            ds.scan();
        } catch (IllegalStateException e) {
            // the source directory (java/) does not exist
            return new String[0];
        }

        return ds.getIncludedFiles();
    }


    public static final void copyFiles(final File dest, final List<File> inputFiles, final List<String> outputFileNames)
            throws IOException {
        if( inputFiles.size() != outputFileNames.size() ) {

            final String inputFilePaths = joinFilePaths(inputFiles);
            final String outputFileNamesStr = StringUtils.join(outputFileNames.iterator(), ", ");

            throw new RuntimeException("Number of input files and file names must be equal! "  +
                    "Dest Dir( " + dest.getAbsolutePath() + ") Input Files (" + inputFilePaths +
                    " ) OutputFileNames (" + outputFileNamesStr + ")"
            );
        }

        for( int i = 0; i < inputFiles.size(); i++ ) {
            FileUtils.copyFile(inputFiles.get(i), new File(dest, outputFileNames.get(i)));
        }
    }

    public static String joinFilePaths(final List<File> files) {
        String inputFilePaths = "";

        boolean comma = false;
        for(final File file : files) {
            if(comma) {
                inputFilePaths += ", ";
            } else {
                comma = true;
            }
            inputFilePaths += file.getAbsolutePath();
        }

        return inputFilePaths;
    }

}
