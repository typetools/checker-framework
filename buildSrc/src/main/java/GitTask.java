import java.io.File;
import java.io.IOException;
import javax.inject.Inject;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

/** An abstract class that is used by other task to do git commands. */
public abstract class GitTask extends DefaultTask {

  /** Used to run exec commands. */
  protected final ExecOperations execOperations;

  /**
   * Creates a new GitTask.
   *
   * @param execOperations used to run exec commands
   */
  @Inject
  public GitTask(ExecOperations execOperations) {
    this.execOperations = execOperations;
  }

  /** Clones or updates a repo. */
  @TaskAction
  public abstract void doTaskAction();

  /**
   * Clones the repository at {@code url}. If the clone fails, sleep 1 minute then retry clone.
   *
   * @param url repository URL
   * @param branch if non-null, which branch to use
   * @param directory where to clone
   */
  public void cloneRetryOnce(String url, String branch, File directory) {
    clone(url, branch, directory, true);
    if (!new File(directory, ".git").exists()) {
      getLogger()
          .warn(
              "Cloning failed, will try again in 1 minute: clone({}, {}, {})",
              url,
              branch,
              directory);
      try {
        Thread.sleep(60000); // wait 1 minute, then try again
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
      clone(url, branch, directory, false);
    }
  }

  /**
   * Quietly clones the git repository at {@code url}, to {@code directory}, with depth 1.
   *
   * @param url git repository to clone
   * @param branch if non-null, which branch to use
   * @param directory where to clone
   * @param ignoreError if true, don't fail the build if the clone command fails
   */
  public void clone(String url, String branch, File directory, boolean ignoreError) {
    CloneCommand cloneCommand =
        Git.cloneRepository().setURI(url).setDirectory(directory).setTimeout(60).setDepth(1);
    if (branch != null) {
      cloneCommand.setBranch(branch);
    }
    try (Git git = cloneCommand.call()) {
      getLogger().debug("Cloning successful.");
    } catch (GitAPIException e) {
      getLogger().warn("Error cloning repository {}: {}", url, e.getMessage());
      if (ignoreError) {
        return;
      }
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * Updates the git clone at {@code directory}. If the update fails, then a warning is printed, but
   * no exception is thrown.
   *
   * @param directory where the clone to update is
   */
  public void update(File directory) {
    try (Git git = Git.open(directory)) {
      git.pull().call();
    } catch (GitAPIException e) {
      //       If the repository remote is configured using ssh, e.g.,
      // git@github.com:typetools/checker-framework.git,
      //       then the above may get permission problems such as:
      //       org.eclipse.jgit.api.errors.TransportException: git@github.com:smillst/jdk.git:
      // invalid privatekey: ...
      //       So fall back to running git pull on the command line.
      org.gradle.process.ExecResult execResult =
          execOperations.exec(
              execSpec -> {
                execSpec.workingDir(directory);
                execSpec.executable("git");
                execSpec.args("pull", "-q");
                execSpec.setIgnoreExitValue(true);
              });
      if (execResult.getExitValue() != 0) {
        getLogger()
            .warn("git pull failed in {} with exit code {}", directory, execResult.getExitValue());
      }
    } catch (IOException e) {
      getLogger().warn("git pull failed in {} because {}", directory, e.getMessage());
    }
  }
}
