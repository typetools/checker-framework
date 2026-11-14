// Test input for ResourceLeakCountMustCallTest.

import java.io.FileInputStream;
import java.io.IOException;

class CountMustCall {
  void leak(String name) throws IOException {
    // The Resource Leak Checker reports required.method.not.called here.
    FileInputStream s = new FileInputStream(name);
  }
}
