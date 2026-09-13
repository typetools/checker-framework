import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Deletes {@code lib/Missing.class} from the test's class directory, so that the following
 * compilations run against a classpath from which one class file is absent. See Issue8055.java.
 */
public class DeleteMissingClassFile {

  public static void main(String[] args) throws IOException {
    Path classes = Paths.get(System.getProperty("test.classes", "."));
    Path missing = classes.resolve("lib").resolve("Missing.class");
    if (!Files.deleteIfExists(missing)) {
      throw new AssertionError("did not find " + missing + " to delete");
    }
  }
}
