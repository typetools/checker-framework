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
 * @author Adam Warski (adam at warski dot org)
 */
public class PathUtils {
	private final static String CHECKERS_GROUPD_ID = "types.checkers";
	private final static String CHECKERS_ARTIFACT_ID = "jsr308-all";

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
	public static String getCheckersJar(String checkersVersion, ArtifactFactory artifactFactory,
										ArtifactResolver artifactResolver, List<?> remoteArtifactRepositories,
										ArtifactRepository localRepository) throws MojoExecutionException {
		Artifact checkersArtifact;
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
	public static String getExecutablePath(String executable, ToolchainManager toolchainManager, MavenSession session) {
		File execFile = new File(executable);
		if (execFile.exists()) {
			return execFile.getAbsolutePath();
		} else {
			Toolchain tc = toolchainManager.getToolchainFromBuildContext("jdk", session);

			if (tc != null) {
				executable = tc.findTool(executable);
			}
		}

		return executable;
	}

	/**
	 * Scans the given compile source roots for sources, taking into account the given includes and excludes.
	 * @param compileSourceRoots A list of source roots. 
	 * @param sourceIncludes Includes specification.
	 * @param sourceExcludes Excludes specification.
	 * @return A list of included sources from the given source roots.
	 */
	public static List<String> scanForSources(List<?> compileSourceRoots, Set<String> sourceIncludes, Set<String> sourceExcludes) {
		if (sourceIncludes.isEmpty()) {
			sourceIncludes.add("**/*.java");
		}

		List<String> sources = new ArrayList<String>();

		for (Object compileSourceRoot : compileSourceRoots) {
			File compileSourceRootFile = new File(compileSourceRoot.toString());
			String[] sourcesFromSourceRoot =
					scanForSources(compileSourceRootFile, sourceIncludes, sourceExcludes);

			for (String sourceFromSourceRoot : sourcesFromSourceRoot) {
				sources.add(new File(compileSourceRootFile, sourceFromSourceRoot).getAbsolutePath());
			}
		}

		return sources;
	}

	private static String[] scanForSources(File sourceDir, Set<String> sourceIncludes, Set<String> sourceExcludes) {
		DirectoryScanner ds = new DirectoryScanner();
		ds.setFollowSymlinks( true );
		ds.setBasedir( sourceDir );

		ds.setIncludes(sourceIncludes.toArray(new String[sourceIncludes.size()]));
		ds.setExcludes(sourceExcludes.toArray(new String[sourceExcludes.size()]));

		ds.addDefaultExcludes();

		ds.scan();

		return ds.getIncludedFiles();
	}
}
