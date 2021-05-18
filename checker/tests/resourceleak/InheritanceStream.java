// A test that checks that an empty @MustCall annotation in a stub on a subclass overrides
// a non-empty one on a superclass (that is being inherited).

import java.io.*;

class InheritanceStream {
  void testBAIS(byte[] buf) {
    new ByteArrayInputStream(buf);
  }

  void testBAOS() {
    new ByteArrayOutputStream();
  }

  void testSR(String buf) {
    new StringReader(buf);
  }

  void testSW() {
    new StringWriter();
  }
}
