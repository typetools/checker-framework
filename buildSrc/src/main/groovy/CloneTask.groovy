import java.time.Duration
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

/**
 * Custom class that clones or updates a given Git repository.
 */
abstract class CloneTask extends DefaultTask {
  private ExecOperations execOperations

  @Inject
  CloneTask(ExecOperations execOperations) {
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
      execOperations.exec {
        workingDir directory
        executable 'git'
        args = ['pull', '-q']
        ignoreExitValue = true
        timeout = Duration.ofMinutes(1)
      }
    } else {
      try {
        clone(url, directory.toString(), true)
      } catch (Throwable t) {
        println "Exception while cloning ${url}"
        t.printStackTrace()
      }
      if (!new File(directory, ".git").exists()) {
        println "Cloning failed, will try again in 1 minute: clone(${url}, ${directory}, true, ${extraArgs})"
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
   * @param extraArgs any extra arguments to pass to git
   */
  void clone(url, directory, ignoreError, extraArgs = []){
    execOperations.exec {
      workingDir "${directory}/../"
      executable 'git'
      args = [
        'clone',
        '-q',
        '--depth=1',
        url,
        new File(directory).toPath().last()
      ]
      args += extraArgs
      ignoreExitValue = ignoreError
      timeout = Duration.ofMinutes(1)
    }
  }
}
