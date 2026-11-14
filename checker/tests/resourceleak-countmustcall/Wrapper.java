// Test input for ResourceLeakCountMustCallTest.

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

class Wrapper {
  void leak(String name) throws IOException {
    // The wrapper and the stream that it wraps are one obligation.
    BufferedInputStream s = new BufferedInputStream(new FileInputStream(name));
  }
}
