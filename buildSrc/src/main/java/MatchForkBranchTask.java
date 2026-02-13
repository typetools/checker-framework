import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import javax.annotation.Nullable;
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
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

/** Custom class that clones or updates a given Git repository. */
public abstract class MatchForkBranchTask extends DefaultTask {

  @Internal
  public abstract Property<File> getDirectory();

  @TaskAction
  public void run() {
    File cfDir = getDirectory().get();
    File jdkDir = new File(cfDir.getParentFile(), "jdk");
    if (jdkDir.exists()) {
      checkBranchFork(cfDir, jdkDir);
      CloneTask.update(jdkDir);
    } else {
      ForkBranch fbCf = findForkBranch(new File(cfDir, ".git"));
      if (fbCf != null) {
        String url = getGitHubUrl(fbCf.fork, "jdk");
        CloneTask.cloneAndUpdate(url, fbCf.branch, jdkDir);
      } else {
        String url = getGitHubUrl("typetools", "jdk");
        CloneTask.cloneAndUpdate(url, "master", jdkDir);
      }
    }
  }

  public void checkBranchFork(File cfDif, File jdkDir) {
    ForkBranch fbCf = findForkBranch(new File(cfDif, ".git"));
    ForkBranch fbJdk = findForkBranch(new File(jdkDir, ".git"));
    System.out.printf("CF: %s JDK: %s%n", fbCf, fbJdk);
    if (fbCf == null || fbJdk == null || fbCf.equals(fbJdk)) {
      // Either CF or JDK is not a clone, or the CF and JDK are using the same fork and branch.
      return;
    }
    if (!forkExists(fbCf.fork, "jdk")) {
      // There is no jdk fork that is the same as CF.
      return;
    }
    if (doesRemoteBranchExist(fbCf.fork, "jdk", fbCf.branch)) {
      throw new RuntimeException(
          String.format(
              "Please checkout the corresponding JDK branch. Fork: %s Branch: %s.",
              getGitHubUrl(fbCf.fork, "jdk"), fbCf.branch));
    }
  }

  public record ForkBranch(String fork, String branch) {

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof ForkBranch(String fork1, String branch1))) {
        return false;
      }

      return fork.equals(fork1) && branch.equals(branch1);
    }

    @Override
    public int hashCode() {
      int result = fork.hashCode();
      result = 31 * result + branch.hashCode();
      return result;
    }
  }

  public @Nullable ForkBranch findForkBranch(File gitDir) {
    try {
      Repository repository =
          new FileRepositoryBuilder().setGitDir(gitDir).readEnvironment().findGitDir().build();

      String branchName = repository.getBranch();
      if (branchName == null) {
        return null;
      }

      Config config = repository.getConfig();

      // Get the remote name (e.g., "origin")
      String remoteName =
          config.getString(
              ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_REMOTE);

      // Get the merge branch name (e.g., "refs/heads/master")
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
            // git@github.com:typetools/checker-framework.git
            fork = remoteUrl.substring("git@github.com:".length(), remoteUrl.indexOf("/"));
          } else {
            // https://github.com/mernst/checker-framework.git
            @SuppressWarnings("deprecation")
            URL url = new URL(remoteUrl);
            String path = url.getPath();
            fork = path.split("/")[1];
          }
          return new ForkBranch(fork, branchName);
        }
      }

    } catch (IOException e) {
      System.err.println("Error cloning repository: " + e.getMessage());
      e.printStackTrace();
    }

    return null;
  }

  public boolean forkExists(final String org, final String reponame) {
    return urlExists(getGitHubUrl(org, reponame));
  }

  static String getGitHubUrl(String org, String repo) {
    return String.format("https://github.com/%s/%s", org, repo);
  }

  public boolean urlExists(String urlAddress) {
    try {
      URL url = new URL(urlAddress);
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

  public static boolean doesRemoteBranchExist(String org, String repo, String branchName) {
    // JGit uses the full internal Git reference name, which for a branch is
    // "refs/heads/<branchName>"
    String fullBranchName = Constants.R_HEADS + branchName;

    try {
      LsRemoteCommand lsRemoteCommand = new LsRemoteCommand(null);

      // Execute the ls-remote command to get all references from the remote
      Collection<Ref> remoteRefs = lsRemoteCommand.setRemote(getGitHubUrl(org, repo)).call();

      // Iterate through the results to find the specific branch
      for (Ref ref : remoteRefs) {
        if (ref.getName().equals(fullBranchName)) {
          return true;
        }
      }

    } catch (Exception e) {
      // Handle exceptions like network issues, authentication failures, etc.
      System.err.println("Error checking remote branch existence: " + e.getMessage());
    }

    return false;
  }
}
