import checkers.quals.*;
import java.io.*;

// Tests related to resource variables in try-with-resources statements.

class ResourceVariables {
  void foo(InputStream arg) {
    try (InputStream in = arg) {
      if (in == null) {
        throw new IllegalArgumentException("Resource not found");
      }
    } catch (IOException e) {
    }
  }
}

