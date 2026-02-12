import javax.inject.Inject
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.ConfigConstants
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

/**
 * Custom class that clones or updates a given Git repository.
 */
abstract class MatchForkBranchTask extends DefaultTask {
  private ExecOperations execOperations

  @Inject
  MatchForkBranchTask(ExecOperations execOperations) {
    this.execOperations = execOperations
  }

  @Input
  def url = project.objects.property(String)

  @OutputDirectory
  def directory = project.objects.property(File)

  @TaskAction
  void doTaskAction() {
    cloneAndUpdate(url.get(), directory.get())
  }

  void cloneAndUpdate(String url, File directory) {
    def fb = findForkBranch(new File(directory, ".git"))
    if(fb != null) {
      printf("Fork: %s, Branch: %s%n", fb[0], fb[1])
    }
  }


  String[] findForkBranch( gitDir) {
    try {
      Repository repository = new FileRepositoryBuilder()
          .setGitDir(gitDir)
          .readEnvironment() // Scan environment GIT_* variables
          .findGitDir() // Call findGitDir() to retrieve the repository's git directory
          .build()


      String branchName = repository.getBranch()
      if (branchName == null) {
        return null
      }

      Config config = repository.getConfig()

      // Get the remote name (e.g., "origin")
      String remoteName = config.getString(
          ConfigConstants.CONFIG_BRANCH_SECTION,
          branchName,
          ConfigConstants.CONFIG_KEY_REMOTE
          )

      // Get the merge branch name (e.g., "refs/heads/master")
      String mergeBranchName = config.getString(
          ConfigConstants.CONFIG_BRANCH_SECTION,
          branchName,
          ConfigConstants.CONFIG_KEY_MERGE
          )


      if (remoteName != null && mergeBranchName != null) {
        // The mergeBranchName is typically "refs/heads/<branch_name>",
        // but the remote tracking branch name is often represented as remoteName/simpleBranchName
        String remoteBranchSimpleName = mergeBranchName.substring(Constants.R_HEADS.length())

        // Get the URL for the "origin" remote (used for fetching and pushing by default)
        String remoteUrl = config.getString("remote", remoteName, "url")

        if (remoteUrl != null && !remoteUrl.isEmpty()) {
          String fork
          if(remoteUrl.startsWith("git@github.com:")) {
            // git@github.com:typetools/checker-framework.git
            fork = remoteUrl.substring("git@github.com:".length(), remoteUrl.indexOf('/'))
          } else {
            // https://github.com/mernst/checker-framework.git
            URL url = new URL(remoteUrl)
            String path = url.getPath()
            fork = path.split('/')[1]
          }
          return [fork, remoteBranchSimpleName]
        }
      }
    } catch (GitAPIException e) {
      System.err.println("Error cloning repository: " + e.getMessage())
      e.printStackTrace()
    }
    return null
  }


  boolean forkExists(org, reponame) {
    return "https://github.com/${org}/${reponame}.git"
  }
  boolean urlExists(String urlAddress) {
    try {
      def url = new URL(urlAddress)
      def connection = url.openConnection() as HttpURLConnection
      // Set request method to HEAD to only fetch headers and reduce load
      connection.setRequestMethod("HEAD")
      // Optional: set a reasonable timeout
      connection.setConnectTimeout(5000) // 5 seconds
      connection.setReadTimeout(5000) // 5 seconds

      int responseCode = connection.getResponseCode()
      // 200 series codes indicate success (200 OK, 204 No Content, etc.)
      return (responseCode >= 200 && responseCode < 300)
    } catch (Exception e) {
      // Catches connection issues, invalid protocols, or non-existent domains
      return false
    }
  }
}
