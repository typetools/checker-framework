// @below-java17-jdk-skip-test
import org.checkerframework.checker.err01.qual.Insecure;

/** Test case for rule ERR01-J: "Check nestedly thrown exceptions" */
public class InsecureThrow {

  @Insecure
  public class SecurityIOException extends Exception {
    /* ... */
  }
  ;

  // :: warning: (warning.insecure)
  public void nestedThrow(String[] args) throws SecurityIOException {
    throw new SecurityIOException();
  }
}
