import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestListener;
import org.gradle.api.tasks.testing.TestResult;

/**
 * When a test fails, prints a message telling the user which task to run. Register this on an
 * internal test task, whose name users should not have to know or type.
 */
public class RerunAdvice implements TestListener {

  /** The name of the task to run, such as ":checker:ainferTestCheckerAjavaTest". */
  private final String taskName;

  /**
   * Creates a RerunAdvice.
   *
   * @param taskName the name of the task to run, such as ":checker:ainferTestCheckerAjavaTest"
   */
  public RerunAdvice(String taskName) {
    this.taskName = taskName;
  }

  @Override
  public void beforeSuite(TestDescriptor suite) {}

  @Override
  public void afterSuite(TestDescriptor suite, TestResult result) {
    // A suite with no parent is the root suite, which completes after every test has run.
    if (suite.getParent() == null && result.getResultType() == TestResult.ResultType.FAILURE) {
      System.out.println(
          "This is an internal task.  After fixing the problem, re-test by running:  ./gradlew "
              + taskName);
    }
  }

  @Override
  public void beforeTest(TestDescriptor testDescriptor) {}

  @Override
  public void afterTest(TestDescriptor testDescriptor, TestResult result) {}
}
