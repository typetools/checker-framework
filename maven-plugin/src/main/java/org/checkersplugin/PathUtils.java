package org.checkersplugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.toolchain.ToolchainManager;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A set of utility methods to find the necessary JSR308 jars and to resolve any sources/classes needed
 * for compilation and checking
 * @author Adam Warski (adam at warski dot org)
 */
public class PathUtils {
    private final static String CHECKERS_GROUPD_ID = "types.checkers";
    private final static String CHECKERS_ARTIFACT_ID = "jsr308-all";
    private final static String DEFAULT_INCLUSION_PATTERN = "**/*.java";

    /**
     * Gets the path to the jsr308-all jar.
     * @param checkersVersion Version of checkers to use.
     * @param artifactFactory Dependency.
     * @param artifactResolver Dependency.
     * @param remoteArtifactRepositories Dependency.
     * @param localRepository Dependency.
     * @return Path to the jsr308-all jar.
     * @throws MojoExecutionException
     */
    public static String getCheckersJar(final String checkersVersion, final ArtifactFactory artifactFactory,
                                        final ArtifactResolver artifactResolver, final List<?> remoteArtifactRepositories,
                                        final ArtifactRepository localRepository) throws MojoExecutionException {
        final Artifact checkersArtifact;
        try {
            checkersArtifact = artifactFactory.createExtensionArtifact(CHECKERS_GROUPD_ID, CHECKERS_ARTIFACT_ID,
                    VersionRange.createFromVersionSpec(checkersVersion));
        } catch (InvalidVersionSpecificationException e) {
            throw new MojoExecutionException("Wrong version: " + checkersVersion + " of checkers specifed.", e);
        }

        try {
            artifactResolver.resolve(checkersArtifact, remoteArtifactRepositories, localRepository);
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException("Unable to find version " + checkersVersion + " of checkers.", e);
        } catch (ArtifactNotFoundException e) {
            throw new MojoExecutionException("Unable to resolve version " + checkersVersion + " of checkers.", e);
        }

        return checkersArtifact.getFile().getPath();
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

        ds.scan();

        return ds.getIncludedFiles();
    }
}
