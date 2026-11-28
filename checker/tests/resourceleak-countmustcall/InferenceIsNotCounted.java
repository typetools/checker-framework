// Test input for ResourceLeakCountMustCallTest.

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

class InferenceIsNotCounted {
  // Not counted, because the parameter is not owning.  Whole-program inference tracks the
  // parameter, but -AcountMustCall does not count what inference tracks.
  static void use(InputStream in) throws IOException {
    BufferedInputStream b = new BufferedInputStream(in);
    b.close();
  }
}
