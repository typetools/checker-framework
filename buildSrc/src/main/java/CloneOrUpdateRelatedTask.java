import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import javax.inject.Inject;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

/**
 * Custom task that clones a related repository. If the related repository has already been cloned,
 * then this task updates (pulls) it and checks that the cloned repository is checked out to the
 * same branch and fork as this one.
 */
public abstract class CloneOrUpdateRelatedTask extends DefaultTask {

  /** The GitHub organization to use to clone the related repository if one is not found. */
  private static final String DEFAULT_ORG = "typetools";

  /** The branch to use to clone the related repository if one is not found. */
  private static final String DEFAULT_BRANCH = "master";

  /**
   * Returns the name of the related repository.
   *
   * @return the name of the related repository
   */
  @Input
  public abstract Property<String> getRelatedRepo();

  /** Used to run exec commands. */
  private final ExecOperations execOperations;

  @Inject
  public CloneOrUpdateRelatedTask(ExecOperations execOperations) {
    this.execOperations = execOperations;
  }

  @TaskAction
  public void doTaskAction() {
    String relatedRepo = getRelatedRepo().get();
    File cfDir = getProject().getRootDir();
    File relatedRepoDir = new File(cfDir.getParentFile(), relatedRepo);
    if (relatedRepoDir.exists()) {
      checkForkBranch(relatedRepoDir);
      CloneOrUpdateTask.update(relatedRepoDir, execOperations);
    } else {
      ForkBranch fbCf = findForkBranch(new File(cfDir, ".git"));
      if (fbCf == null
          || !forkExists(fbCf.fork, relatedRepo)
          || !remoteBranchExists(fbCf.fork, relatedRepo, fbCf.branch)) {
        fbCf = new ForkBranch(DEFAULT_ORG, DEFAULT_BRANCH);
      }
      String url = getGitHubUrl(fbCf.fork, relatedRepo);
      CloneOrUpdateTask.cloneRetryOnce(url, fbCf.branch, relatedRepoDir);
    }
  }

  /**
   * Check that the {@code relatedRepo} is checked out to the same fork and branch as {@code cfDir}.
   *
   * @param relatedRepoDir a related repository
   */
  private void checkForkBranch(File relatedRepoDir) {
    File cfDir = getProject().getRootDir();

    String relatedRepo = getRelatedRepo().get();
    ForkBranch fbCf = findForkBranch(new File(cfDir, ".git"));
    ForkBranch fbRelated = findForkBranch(new File(relatedRepoDir, ".git"));

    if (fbCf == null || fbRelated == null || fbCf.equals(fbRelated)) {
      // Either CF or related is not a clone, or the CF and related are using the same fork and
      // branch.
      return;
    }
    if (!forkExists(fbCf.fork, relatedRepo)) {
      // There is no related fork that is the same as the CF fork.
      return;
    }
    if (remoteBranchExists(fbCf.fork, relatedRepo, fbCf.branch)) {
      throw new RuntimeException(
          String.format(
              "Please checkout the corresponding %s branch. Fork: %s Branch: %s.",
              relatedRepo, getGitHubUrl(fbCf.fork, relatedRepo), fbCf.branch));
    }
  }

  public record ForkBranch(String fork, String branch) {}

  /**
   * Find the fork and branch of the remote tracking branch that is currently checked out in {@code
   * gitDir}. If the branch checked out at {@code gitDir} does not have a remote tracking branch,
   * returns {@code null}.
   *
   * @param gitDir a git directory
   * @return the fork and branch of {@code gitDir} or null if there is no remote branch
   */
  private ForkBranch findForkBranch(File gitDir) {
    try (Repository repository =
        new FileRepositoryBuilder().setGitDir(gitDir).readEnvironment().findGitDir().build()) {

      String branchName = repository.getBranch();
      if (branchName == null) {
        return null;
      }

      Config config = repository.getConfig();
      String remoteName =
          config.getString(
              ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_REMOTE);
      String mergeBranchName =
          config.getString(
              ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_MERGE);

      if (remoteName != null && mergeBranchName != null) {
        // Get the URL for the "origin" remote (used for fetching and pushing by default).
        String remoteUrl =
            config.getString(ConfigConstants.CONFIG_REMOTE_SECTION, remoteName, "url");

        if (remoteUrl != null && !remoteUrl.isEmpty()) {
          String fork;
          if (remoteUrl.startsWith("git@github.com:")) {
            if (!remoteUrl.contains("/")) {
              return null;
            }
            // `remoteUrl` has the form:
            // git@github.com:typetools/checker-framework.git
            fork = remoteUrl.substring("git@github.com:".length(), remoteUrl.indexOf("/"));
          } else {
            // `remoteUrl` has the form:
            // https://github.com/mernst/checker-framework.git
            URL url = URI.create(remoteUrl).toURL();
            String path = url.getPath();
            if (!path.contains("/")) {
              return null;
            }
            fork = path.split("/")[1];
          }
          return new ForkBranch(fork, branchName);
        }
      }

    } catch (IOException | IllegalArgumentException e) {
      System.err.println("Error finding branch: " + e.getMessage());
    }

    return null;
  }

  /**
   * Returns true if "https://github.com/{@code org}/{@code repo}" exists
   *
   * @param org a GitHub organization
   * @param repo a repository in the {@code org}.
   * @return true if "https://github.com/{@code org}/{@code repo}" exists
   */
  private boolean forkExists(final String org, final String repo) {
    return urlExists(getGitHubUrl(org, repo));
  }

  /**
   * Returns the GitHub URL formatted as "https://github.com/{@code org}/{@code repo}"
   *
   * @param org a GitHub organization
   * @param repo a repository in {@code org}
   * @return the GitHub URL formatted as "https://github.com/{@code org}/{@code repo}"
   */
  private static String getGitHubUrl(String org, String repo) {
    return String.format("https://github.com/%s/%s", org, repo);
  }

  /**
   * Returns true if {@code urlAddress} exists.
   *
   * @param urlAddress a URL
   * @return true if {@code urlAddress} exists
   */
  private boolean urlExists(String urlAddress) {
    HttpURLConnection connection = null;
    try {
      connection = (HttpURLConnection) URI.create(urlAddress).toURL().openConnection();
      connection.setRequestMethod("HEAD");

      int responseCode = connection.getResponseCode();
      return HttpURLConnection.HTTP_OK == responseCode;
    } catch (Exception e) {
      return false;
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  /**
   * Returns true if branch {@code branchName} exists on "https://github.com/{@code org}/{@code
   * repo}".
   *
   * @param org a GitHub organization
   * @param repo a repository in {@code org}
   * @param branchName a name of a branch
   * @return true if branch {@code branchName} exists on "https://github.com/{@code org}/{@code
   *     repo}"
   */
  private static boolean remoteBranchExists(String org, String repo, String branchName) {
    // JGit uses the full internal Git reference name, which for a branch is
    // "refs/heads/<branchName>".
    String fullBranchName = Constants.R_HEADS + branchName;

    try {
      // Execute the ls-remote command to get all references from the remote
      Collection<Ref> remoteRefs =
          new LsRemoteCommand(null)
              .setRemote(getGitHubUrl(org, repo))
              .setTimeout(60)
              .setHeads(true)
              .call();
      for (Ref ref : remoteRefs) {
        if (ref.getName().equals(fullBranchName)) {
          return true;
        }
      }

    } catch (Exception e) {
      System.err.println("Error checking remote branch existence: " + e.getMessage());
    }

    return false;
  }
}
