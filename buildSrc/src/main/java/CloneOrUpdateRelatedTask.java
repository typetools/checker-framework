import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
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
 * Custom task that clones or updates a related repository. If the related repository has already
 * been cloned, then this task checks that the cloned repository is checked out to the same branch
 * and fork as this one.
 */
public abstract class CloneOrUpdateRelatedTask extends DefaultTask {

  /** The GitHub origination to use to clone the related repository if one is not found. */
  String defaultOrg = "typetools";

  /** The branch to use to clone the related repository if one is not found. */
  String defaultBranch = "master";

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
      checkBranchFork(relatedRepoDir);
      CloneOrUpdateTask.update(relatedRepoDir, execOperations);
    } else {
      ForkBranch fbCf = findForkBranch(new File(cfDir, ".git"));
      if (fbCf == null) {
        fbCf = new ForkBranch(defaultOrg, defaultBranch);
      }
      String url = getGitHubUrl(fbCf.fork, relatedRepo);
      CloneOrUpdateTask.cloneRetryOnce(url, fbCf.branch, relatedRepoDir);
    }
  }

  /**
   * Check that the {@code relatedRepo} is checked out to the same fork/branch as {@code cfDir}.
   *
   * @param relatedRepoDir a related repo
   */
  private void checkBranchFork(File relatedRepoDir) {
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
    if (doesRemoteBranchExist(fbCf.fork, relatedRepo, fbCf.branch)) {
      throw new RuntimeException(
          String.format(
              "Please checkout the corresponding %s branch. Fork: %s Branch: %s.",
              relatedRepo, getGitHubUrl(fbCf.fork, relatedRepo), fbCf.branch));
    }
  }

  public record ForkBranch(String fork, String branch) {

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof ForkBranch other)) {
        return false;
      }

      return Objects.equals(fork, other.fork) && Objects.equals(branch, other.branch);
    }

    @Override
    public int hashCode() {
      int result = fork.hashCode();
      result = 31 * result + branch.hashCode();
      return result;
    }
  }

  /**
   * Find the fork and branch of the remote tracking branch that is currently checked out in {@code
   * gitDir}. If the branch checked out at {@code gitDir} does not have a remote tracking branch,
   * then {@code null} is returned.
   *
   * @param gitDir a git directory
   * @return the fork and branch of {@code gitDir} or null if there is no remote branch.
   */
  private @Nullable ForkBranch findForkBranch(File gitDir) {
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
        // The mergeBranchName is typically "refs/heads/<branch_name>",
        // but the remote tracking branch name is often represented as remoteName/simpleBranchName
        String remoteBranchSimpleName = mergeBranchName.substring(Constants.R_HEADS.length());

        // Get the URL for the "origin" remote (used for fetching and pushing by default)
        String remoteUrl = config.getString("remote", remoteName, "url");

        if (remoteUrl != null && !remoteUrl.isEmpty()) {
          String fork;
          if (remoteUrl.startsWith("git@github.com:")) {
            // Urls like:
            // git@github.com:typetools/checker-framework.git
            fork = remoteUrl.substring("git@github.com:".length(), remoteUrl.indexOf("/"));
          } else {
            // Urls like:
            // https://github.com/mernst/checker-framework.git
            URL url = URI.create(remoteUrl).toURL();
            String path = url.getPath();
            fork = path.split("/")[1];
          }
          return new ForkBranch(fork, branchName);
        }
      }

    } catch (IOException e) {
      System.err.println("Error cloning repository: " + e.getMessage());
    }

    return null;
  }

  /**
   * Returns true if "https://github.com/{@code org}/{@code repo}" exists
   *
   * @param org a GitHub organization
   * @param repo a repository in the {@org}.
   * @return true if "https://github.com/{@code org}/{@code repo}" exists
   */
  private boolean forkExists(final String org, final String repo) {
    return urlExists(getGitHubUrl(org, repo));
  }

  /**
   * Returns the GitHub url formated as "https://github.com/{@code org}/{@code repo}"
   *
   * @param org a GitHub organization
   * @param repo a repository in the {@org}
   * @return the GitHub url formated as "https://github.com/{@code org}/{@code repo}"
   */
  private static String getGitHubUrl(String org, String repo) {
    return String.format("https://github.com/%s/%s", org, repo);
  }

  /**
   * Returns true if {@code urlAddress} exists.
   *
   * @param urlAddress a url
   * @return true if {@code urlAddress} exists
   */
  private boolean urlExists(String urlAddress) {
    try {
      URL url = URI.create(urlAddress).toURL();
      HttpURLConnection connection =
          DefaultGroovyMethods.asType(url.openConnection(), HttpURLConnection.class);
      // Set request method to HEAD to only fetch headers and reduce load
      connection.setRequestMethod("HEAD");
      // Optional: set a reasonable timeout
      connection.setConnectTimeout(5000); // 5 seconds
      connection.setReadTimeout(5000); // 5 seconds

      int responseCode = connection.getResponseCode();
      // 200 series codes indicate success (200 OK, 204 No Content, etc.)
      return (responseCode >= 200 && responseCode < 300);
    } catch (Exception e) {
      // Catches connection issues, invalid protocols, or non-existent domains
      return false;
    }
  }

  /**
   * Returns true if the {@code branchName} exist on "https://github.com/{@code org}/{@code repo}".
   *
   * @param org a GitHub organization
   * @param repo a repository in the {@org}.
   * @param branchName a name of a branch
   * @return true if the {@code branchName} exist on "https://github.com/{@code org}/{@code repo}
   */
  private static boolean doesRemoteBranchExist(String org, String repo, String branchName) {
    // JGit uses the full internal Git reference name, which for a branch is
    // "refs/heads/<branchName>"
    String fullBranchName = Constants.R_HEADS + branchName;

    try {
      // Execute the ls-remote command to get all references from the remote
      Collection<Ref> remoteRefs =
          new LsRemoteCommand(null).setRemote(getGitHubUrl(org, repo)).call();
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
