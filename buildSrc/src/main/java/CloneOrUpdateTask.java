import java.io.File;
import javax.inject.Inject;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

/** A task that clones or updates a given Git repository. */
public abstract class CloneOrUpdateTask extends GitTask {

  /**
   * The URL to clone or update.
   *
   * @return the URL to clone or update
   */
  @Input
  public abstract Property<String> getUrl();

  /**
   * Directory for the clone.
   *
   * @return directory for the clone
   */
  @OutputDirectory
  public abstract DirectoryProperty getDirectory();

  /**
   * Creates a new CloneOrUpdateTask.
   *
   * @param execOperations used to run exec commands
   */
  @Inject
  public CloneOrUpdateTask(ExecOperations execOperations) {
    super(execOperations);
  }

  /** Clones or updates a repo. */
  @TaskAction
  public void doTaskAction() {
    String url = getUrl().get();
    File directory = getDirectory().get().getAsFile();

    if (new File(directory, ".git").exists()) {
      update(directory);
    } else {
      cloneRetryOnce(url, null, directory);
    }
  }
}
