import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations


abstract class InsertAnnotationsToSource extends DefaultTask {
  private final ExecOperations execOperations

  @Inject
  InsertAnnotationsToSource(ExecOperations execOperations) {
    this.execOperations = execOperations
  }

  @Input
  def testDir = project.objects.property(String)

  @Input
  def classpath = project.objects.property(String)

  @Input
  def afuDir = project.objects.property(String)

  /**
   * The name of the task that users should run, such as ":checker:ainferTestCheckerJaifTest".  If
   * this task fails, users are told to run that task.
   */
  @Input
  def rerunTaskName = project.objects.property(String)


  @TaskAction
  void doTaskAction() {
    try {
      insertAnnotations()
    } catch (Throwable t) {
      println("This is an internal task.  After fixing the problem, re-test by running:  ./gradlew ${rerunTaskName}")
      throw t
    }
  }

  /** Inserts the annotations from the .jaif files into the .java files. */
  private void insertAnnotations() {
    String jaifsDir = testDir+'/inference-output'
    List<File> jaifs = project.fileTree(jaifsDir).matching {
      include '*.jaif'
    }.asList()
    if (jaifs.isEmpty()) {
      throw new GradleException("no .jaif files found in ${jaifsDir}")
    }

    String javasDir = testDir+'/annotated'
    List<File> javas = project.fileTree(javasDir).matching {
      include '*.java'
    }.asList()
    if (javas.isEmpty()) {
      throw new GradleException("no .java files found in ${javasDir}")
    }

    execOperations.exec {
      executable "${afuDir}/bin/insert-annotations-to-source"
      // Script argument -cp must precede Java program argument -i.
      // checker-qual is needed for Constant Value Checker annotations.
      // Note that "/" works on Windows as well as on Linux.
      args = [
        '-cp',
        classpath + ":"+ project.file('tests/build/testclasses')
      ]
      args += ['-i']
      for (File jaif : jaifs) {
        args += [jaif.toString()]
      }
      for (File javaFile : javas) {
        args += [javaFile.toString()]
      }
    }
  }
}
