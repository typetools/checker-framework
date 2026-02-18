import java.io.File;
import java.io.IOException;
import javax.inject.Inject;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

/** Custom class that clones or updates a given Git repository. */
public abstract class CloneOrUpdateTask extends DefaultTask {

  /**
   * The URL to clone or update.
   *
   * @return url to clone or update.
   */
  @Input
  public abstract Property<String> getUrl();

  /**
   * Directory into which to clone or if it exists to pull in new changes.
   *
   * @return directory into which to clone or if it exists to pull in new changes
   */
  @OutputDirectory
  public abstract DirectoryProperty getDirectory();

  /** Used to run exec commands. */
  private ExecOperations execOperations;

  @Inject
  public CloneOrUpdateTask(ExecOperations execOperations) {
    this.execOperations = execOperations;
  }

  @TaskAction
  public void doTaskAction() {
    String url = getUrl().get();
    File directory = getDirectory().get().getAsFile();

    // Gradle creates the directory if it does not exist, so check to see if the directory has a
    // .git directory.
    if (new File(directory, ".git").exists()) {
      update(directory, execOperations);
    } else {
      cloneRetryOnce(url, null, directory);
    }
  }

  /**
   * Clones the repository at {@code url}. If the clone fails, sleep 1 minute then retry clone.
   *
   * @param url repository url
   * @param branch if non-null, which branch to use.
   * @param directory where to clone
   */
  public static void cloneRetryOnce(String url, String branch, File directory) {
    clone(url, branch, directory, true);
    if (!new File(directory, ".git").exists()) {
      System.out.printf(
          "Cloning failed, will try again in 1 minute: clone(%s, %s, true)", url, directory);
      try {
        Thread.sleep(60000); // wait 1 minute, then try again
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      clone(url, branch, directory, false);
    }
  }

  /**
   * Quietly clones the given git repository, {@code url}, to {@code directory} at a depth of 1.
   *
   * @param url git repository to clone
   * @param branch if non-null, which branch to use.
   * @param directory where to clone
   * @param ignoreError whether to fail the build if the clone command fails
   */
  public static void clone(String url, String branch, File directory, boolean ignoreError) {
    CloneCommand cloneCommand =
        Git.cloneRepository().setURI(url).setDirectory(directory).setTimeout(60).setDepth(1);
    if (branch != null) {
      cloneCommand.setBranch(branch);
    }
    try (Git git = cloneCommand.call()) {
      System.out.println("Cloning successful.");
    } catch (GitAPIException e) {
      System.err.println("Error cloning repository: " + e.getMessage());
      if (ignoreError) {
        return;
      }
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * Updates the git repository at {@code directory}.
   *
   * @param directory where the repository to update is
   * @param execOperations used to run exec commands
   */
  public static void update(File directory, ExecOperations execOperations) {
    try {
      Git git = Git.open(directory);
      git.pull().call();
      git.close();
    } catch (GitAPIException e) {
      //       If the repository remote is configured using ssh, i.e.,
      // git@github.com:typetools/checker-framework.git,
      //       then the above may get permission problems such as:
      //       org.eclipse.jgit.api.errors.TransportException: git@github.com:smillst/jdk.git:
      // invalid privatekey: ...
      //       So fall back to running git pull on the command line.
      execOperations.exec(
          execSpec -> {
            execSpec.workingDir(directory);
            execSpec.executable("git");
            execSpec.args("pull", "-q");
            execSpec.setIgnoreExitValue(true);
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
