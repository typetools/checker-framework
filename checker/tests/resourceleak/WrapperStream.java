// A simple test that must-call as a type annotation fixes the simplest version
// of the wrapper stream problem.

import java.io.*;

class WrapperStream {
  void test(byte[] buf) {
    InputStream is = new ByteArrayInputStream(buf);
  }
}
