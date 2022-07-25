import org.checkerframework.framework.testchecker.util.Odd;

// @above-java17-skip-test the error message changed in JDK 18

public class AnnotatedVoidMethodJdk17 {
  // :: error: annotation type not applicable to this kind of declaration
  public @Odd void method() {
    return;
  }
}
