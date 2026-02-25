import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;
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
import org.gradle.api.tasks.UntrackedTask;
import org.gradle.process.ExecOperations;

/**
 * A task that clones a repository that is related to this one, or pulls it if it has already been
 * cloned.
 *
 * <p>Sometimes, two GitHub repositories are related: you need clones of both of them. When run from
 * a clone of one, this clones the other, attempting to find a matching org and branch.
 */
@UntrackedTask(because = "Always try to update.")
public abstract class CloneOrUpdateRelatedTask extends DefaultTask {

  /**
   * The GitHub organization to use to clone the related repository if a matching org is not found.
   */
  private static final String DEFAULT_ORG = "typetools";

  /** The branch to use to clone the related repository if a matching branch is not found */
  private static final String DEFAULT_BRANCH = "master";

  /**
   * Returns the repository name without the organization.
   *
   * @return the repository name without the organization
   */
  @Input
  public abstract Property<String> getRelatedRepo();

  /** Used to run exec commands. */
  private final ExecOperations execOperations;

  /**
   * Constructor.
   *
   * @param execOperations Used to run exec commands
   */
  @Inject
  public CloneOrUpdateRelatedTask(ExecOperations execOperations) {
    this.execOperations = execOperations;
  }

  /** Clones or updates a related repo. */
  @TaskAction
  public void doTaskAction() {
    String relatedRepoName = getRelatedRepo().get();
    File cfDir = getProject().getRootDir();
    File relatedRepoDir = new File(cfDir.getParentFile(), relatedRepoName);
    if (relatedRepoDir.exists() && new File(relatedRepoDir, ".git").exists()) {
      checkOrgBranch(relatedRepoDir);
      CloneOrUpdateTask.update(relatedRepoDir, execOperations);
    } else {
      OrgBranch fbCf = findOrgBranch(new File(cfDir, ".git"));
      if (fbCf == null
          || !orgExists(fbCf.org, relatedRepoName)
          || !remoteBranchExists(fbCf.org, relatedRepoName, fbCf.branch)) {
        fbCf = new OrgBranch(DEFAULT_ORG, DEFAULT_BRANCH);
      }
      String url = getGitHubUrl(fbCf.org, relatedRepoName);
      CloneOrUpdateTask.cloneRetryOnce(url, fbCf.branch, relatedRepoDir);
    }
  }

  /**
   * Check that the {@code relatedRepo} is checked out to the same org and branch as the root
   * directory of this project.
   *
   * @param relatedRepoDir a related repository
   */
  private void checkOrgBranch(File relatedRepoDir) {
    File cfDir = getProject().getRootDir();

    String relatedRepoName = getRelatedRepo().get();
    OrgBranch fbCf = findOrgBranch(new File(cfDir, ".git"));
    OrgBranch fbRelated = findOrgBranch(new File(relatedRepoDir, ".git"));

    if (fbCf == null || fbRelated == null || fbCf.equals(fbRelated)) {
      // Either CF or related is not a clone, or the CF and related are using the same org and
      // branch.
      return;
    }
    if (!orgExists(fbCf.org, relatedRepoName)) {
      // There is no related org that is the same as the CF org.
      return;
    }
    if (remoteBranchExists(fbCf.org, relatedRepoName, fbCf.branch)) {
      throw new RuntimeException(
          String.format(
              "Please checkout the corresponding %s branch. URL: %s Branch: %s.",
              relatedRepoName, getGitHubUrl(fbCf.org, relatedRepoName), fbCf.branch));
    }
  }

  /**
   * A pair of {@code org} and {@code branch}
   *
   * @param org an org, that's an organization in GitHub
   * @param branch a branch of the org.
   */
  public record OrgBranch(String org, String branch) {}

  /**
   * Find the org and branch of the remote tracking branch that is currently checked out in {@code
   * gitDir}. If the branch checked out at {@code gitDir} does not have a remote tracking branch,
   * returns {@code null}.
   *
   * @param gitDir a git directory
   * @return the org and branch of {@code gitDir} or null if there is no remote branch
   */
  private OrgBranch findOrgBranch(File gitDir) {
    try (Repository repository =
        new FileRepositoryBuilder().setGitDir(gitDir).readEnvironment().findGitDir().build()) {

      String currentBranchName = repository.getBranch();
      if (currentBranchName == null) {
        return null;
      }

      Config config = repository.getConfig();

      // The "name" of the remote repository that the current branch is tracking. (This is a name
      // given to the repo by the user when configuring a remote repo.)
      String remoteRepoName =
          config.getString(
              ConfigConstants.CONFIG_BRANCH_SECTION,
              currentBranchName,
              ConfigConstants.CONFIG_KEY_REMOTE);

      // The full name of the branch that the current branch is tracking of the form
      // "refs/heads/branchname".
      String remoteBranchFullName =
          config.getString(
              ConfigConstants.CONFIG_BRANCH_SECTION,
              currentBranchName,
              ConfigConstants.CONFIG_KEY_MERGE);

      if (remoteRepoName != null && remoteBranchFullName != null) {
        String remoteUrl =
            config.getString(ConfigConstants.CONFIG_REMOTE_SECTION, remoteRepoName, "url");

        if (remoteUrl != null && !remoteUrl.isEmpty()) {
          String org;
          if (remoteUrl.startsWith("git@github.com:")) {
            if (!remoteUrl.contains("/")) {
              System.err.println("Unexpected URL format " + remoteUrl);
              return null;
            }
            // `remoteUrl` has the form:
            // git@github.com:typetools/checker-framework.git
            org = remoteUrl.substring("git@github.com:".length(), remoteUrl.indexOf("/"));
          } else {
            // `remoteUrl` has the form:
            // https://github.com/mernst/checker-framework.git
            URL url = URI.create(remoteUrl).toURL();
            String path = url.getPath();
            if (!path.contains("/")) {
              System.err.println("Unexpected URL format " + remoteUrl);
              return null;
            }
            org = path.split("/")[1];
          }
          return new OrgBranch(org, remoteBranchFullName.substring(Constants.R_HEADS.length()));
        }
      }

    } catch (IOException | IllegalArgumentException e) {
      System.err.println("Error finding branch: " + e.getMessage());
    }

    return null;
  }

  /**
   * Returns true if "https://github.com/{@code org}/{@code repoName}" exists
   *
   * @param org a GitHub organization
   * @param repoName a repository in the {@code org}.
   * @return true if "https://github.com/{@code org}/{@code repoName}" exists
   */
  private boolean orgExists(final String org, final String repoName) {
    return urlExists(getGitHubUrl(org, repoName));
  }

  /**
   * Returns the GitHub URL formatted as "https://github.com/{@code org}/{@code repoName}"
   *
   * @param org a GitHub organization
   * @param repoName a repository in {@code org}
   * @return the GitHub URL formatted as "https://github.com/{@code org}/{@code repoName}"
   */
  private static String getGitHubUrl(String org, String repoName) {
    return String.format("https://github.com/%s/%s", org, repoName);
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
   * repoName}".
   *
   * @param org a GitHub organization
   * @param repoName a repository in {@code org}
   * @param branchName a name of a branch
   * @return true if branch {@code branchName} exists on "https://github.com/{@code org}/{@code
   *     repoName}"
   */
  private static boolean remoteBranchExists(String org, String repoName, String branchName) {
    // JGit uses the full internal Git reference name, which for a branch is
    // "refs/heads/<branchName>".
    String fullBranchName = Constants.R_HEADS + branchName;

    try {
      // Execute the ls-remote command to get all references from the remote
      Map<String, Ref> remoteRefs =
          new LsRemoteCommand(null)
              .setRemote(getGitHubUrl(org, repoName))
              .setTimeout(60)
              .setHeads(true)
              .callAsMap();
      return remoteRefs.containsKey(fullBranchName);
    } catch (Exception e) {
      System.err.println("Error checking remote branch existence: " + e.getMessage());
    }

    return false;
  }
}
