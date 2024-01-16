// @below-java17-jdk-skip-test
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/** Test case for rule ERR01-J: "Check nestedly thrown exceptions" */
public class NestedThrow2 {

  public void nestedThrow(String[] args) {
    try {
      FileInputStream fis = new FileInputStream(System.getenv("APPDATA") + args[0]);

      // :: warning: (warning.file_not_found)
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
