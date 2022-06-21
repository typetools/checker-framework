import java.io.FileWriter;
import java.io.IOException;
import org.checkerframework.checker.mustcall.qual.Owning;

public class StaticOwningFieldOtherClass {}

abstract class HasStaticOwningField {
  public static @Owning FileWriter log = null;
}

class TestUtils {
  public static void setLog(String filename) {
    try {
      HasStaticOwningField.log = new FileWriter(filename);
    } catch (IOException ioe) {
      // TODO: clarify that this is a user error
      throw new Error("Cannot write file " + filename, ioe);
    }
  }
}
