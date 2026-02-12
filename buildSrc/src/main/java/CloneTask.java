import java.io.File;
import java.io.IOException;
import org.codehaus.groovy.runtime.DefaultGroovyStaticMethods;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

/** Custom class that clones or updates a given Git repository. */
public abstract class CloneTask extends DefaultTask {

  @Input
  public abstract Property<String> getUrl();

  @OutputDirectory
  public abstract Property<File> getDirectory();

  @TaskAction
  public void doTaskAction() {
    cloneAndUpdate(getUrl().get(), null, getDirectory().get());
  }

  public static void cloneAndUpdate(final String url, String branch, final File directory) {
    //    SshSessionFactory.getInstance();
    // Gradle creates the directory if it does not exist, so check to see if the director has a .git
    // directory.
    if (new File(directory, ".git").exists()) {
      update(directory);
    } else {
      try {
        clone(url, branch, directory, true);
      } catch (Throwable t) {
        System.out.println("Exception while cloning " + url);
        t.printStackTrace();
      }

      if (!new File(directory, ".git").exists()) {
        System.out.printf(
            "Cloning failed, will try again in 1 minute: clone(%s, %s, true)", url, directory);
        DefaultGroovyStaticMethods.sleep(null, 60000); // wait 1 minute, then try again
        clone(url, branch, directory, false);
      }
    }
  }

  /**
   * Quietly clones the given git repository, {@code url}, to {@directory} at a depth of 1.
   *
   * @param url git repository to clone
   * @param branch
   * @param directory where to clone
   * @param ignoreError whether to fail the build if the clone command fails
   */
  public static void clone(String url, String branch, Object directory, Boolean ignoreError) {

    try {
      CloneCommand cloneCommand =
          Git.cloneRepository()
              .setURI(url)
              .setDirectory((File) directory)
              .setTimeout(60)
              .setDepth(1);
      if (branch != null) {
        cloneCommand.setBranch(branch);
      }
      Git git = cloneCommand.call();
      System.out.println("Cloning successful.");
      // Remember to close the Git object when finished
      git.close();
    } catch (GitAPIException e) {
      System.err.println("Error cloning repository: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public static void update(File directory) {
    try {
      SshSessionFactory.getInstance();

      Git git = Git.open(directory);
      git.pull().call();
      git.close();
    } catch (GitAPIException e) {
      System.err.println("Error cloning repository: " + e.getMessage());
      e.printStackTrace();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
