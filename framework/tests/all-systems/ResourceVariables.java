import java.io.*;

// Tests related to resource variables in try-with-resources statements.

public class ResourceVariables {
  void foo(InputStream arg) {
    try (InputStream in = arg) {
    } catch (IOException e) {
    }
  }
}
