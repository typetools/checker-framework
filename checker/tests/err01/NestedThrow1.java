// @below-java17-jdk-skip-test
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/** Test case for rule ERR01-J: "Check nestedly thrown exceptions" */
public class NestedThrow1 {

  public class SecurityIOException extends IOException {
    /* ... */
  }
  ;

  public void nestedThrow(String[] args) throws SecurityIOException {
    try {
      FileInputStream fis = new FileInputStream(System.getenv("APPDATA") + args[0]);

      // :: warning: (warning.file_not_found)
    } catch (FileNotFoundException e) {
      // Log the exception
      throw new SecurityIOException();
    }
  }
}
