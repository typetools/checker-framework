import java.io.File;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

public abstract class InsertAnnotationsToSource extends DefaultTask {
  @Input
  public abstract Property<String> getTestDir();

  @Input
  public abstract Property<String> getClasspath();

  @Input
  public abstract Property<String> getAfuDir();

  private ExecOperations execOperations;

  @Inject
  public InsertAnnotationsToSource(ExecOperations execOperations) {
    this.execOperations = execOperations;
  }

  @TaskAction
  public void doTaskAction() {
    String testDir = getTestDir().get();
    String jaifsDir = testDir + "/inference-output";
    FileCollection jaifs =
        getProject().fileTree(jaifsDir).filter(file -> file.getName().matches(".*\\.jaif"));
    if (jaifs.isEmpty()) {
      throw new GradleException("no .jaif files found in ${jaifsDir}");
    }
    FileCollection javas =
        getProject()
            .fileTree(testDir + "/annotated")
            .filter(file -> file.getName().matches(".*\\.java"));

    if (javas.isEmpty()) {
      throw new GradleException("no .java files found in " + jaifsDir);
    }

    execOperations.exec(
        execSpec -> {
          execSpec.executable(getAfuDir() + "/bin/insert-annotations-to-source");
          // Script argument -cp must precede Java program argument -i.
          // checker-qual is needed for Constant Value Checker annotations.
          // Note that "/" works on Windows as well as on Linux.
          String classpath =
              String.format("%s,%s", getClasspath(), getProject().file("tests/build/testclasses"));
          execSpec.args("-cp", classpath, "-i");
          for (File jaif : jaifs) {
            execSpec.args(jaif.toString());
          }

          for (File javaFile : javas) {
            execSpec.args(javaFile.toString());
          }
        });
  }
}
