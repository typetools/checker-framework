import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

/** Custom class that clones or updates a given Git repository. */
public abstract class MatchForkBranchTask extends DefaultTask {

  @Input
  public abstract Property<String> getUrl();

  @OutputDirectory
  public abstract Property<File> getDirectory();

  @TaskAction
  public void run() {
    cloneAndUpdate(getUrl().get(), getDirectory().get());
  }

  public void cloneAndUpdate(String url, File directory) {
    String[] fb = findForkBranch(new File(directory, ".git"));
    if (fb != null) {
      DefaultGroovyMethods.printf(this, "Fork: %s, Branch: %s%n", new Object[] {fb[0], fb[1]});
      if (forkExists(fb[0], "jdk")) {
        DefaultGroovyMethods.println(this, "found");
      }
    }
  }

  public String[] findForkBranch(File gitDir) {
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
            URL url = new URL(remoteUrl);
            String path = url.getPath();
            fork = path.split("/")[1];
          }
          return new String[] {fork, remoteBranchSimpleName};
        }
      }

    } catch (IOException e) {
      System.err.println("Error cloning repository: " + e.getMessage());
      e.printStackTrace();
    }

    return null;
  }

  public boolean forkExists(final String org, final String reponame) {
    return urlExists(
        "https://github.com/"
            + DefaultGroovyMethods.invokeMethod(String.class, "valueOf", new Object[] {org})
            + "/"
            + DefaultGroovyMethods.invokeMethod(String.class, "valueOf", new Object[] {reponame})
            + ".git");
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
}
