import javax.inject.Inject
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
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
    // Gradle creates the directory if it does not exist, so check to see if the director has a .git directory.
    if (new File(directory, ".git").exists()) {
      update(directory)
    } else {
      try {
        clone(url, directory, true)
      } catch (Throwable t) {
        println "Exception while cloning ${url}"
        t.printStackTrace()
      }
      if (!new File(directory, ".git").exists()) {
        println "Cloning failed, will try again in 1 minute: clone(${url}, ${directory}, true)"
        sleep(60000) // wait 1 minute, then try again
        clone(urlS, getDirectory(), false)
      }
    }
  }
  /**
   * Quietly clones the given git repository, {@code url}, to {@directory} at a depth of 1.
   * @param url git repository to clone
   * @param directory where to clone
   * @param ignoreError whether to fail the build if the clone command fails
   */
  void clone(url, directory, ignoreError) {

    try {
      Git git = Git.cloneRepository()
          .setURI(url)
          .setDirectory(directory)
          .setTimeout(60) // TODO: What units is this?
          .setDepth(1)
          .call()
      System.out.println("Cloning successful.")
      // Remember to close the Git object when finished
      git.close()
    } catch (GitAPIException e) {
      System.err.println("Error cloning repository: " + e.getMessage())
      e.printStackTrace()
    }
  }
  void update( directory) {
    try {
      Git git = Git.open(directory)
      git.pull().call()
      git.close()
    } catch (GitAPIException e) {
      System.err.println("Error cloning repository: " + e.getMessage())
      e.printStackTrace()
    }
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
