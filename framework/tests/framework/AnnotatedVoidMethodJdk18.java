import org.checkerframework.framework.testchecker.util.Odd;

// @below-java18-skip-test the error message changed in JDK 18

public class AnnotatedVoidMethodJdk18 {
  // :: error: annotation interface not applicable to this kind of declaration
  public @Odd void method() {
    return;
  }
}
