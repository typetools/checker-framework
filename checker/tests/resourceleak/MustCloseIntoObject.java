// Test that assigning a "must close" value into a class without a mustCall annotation
// still results in an error.

import java.net.Socket;

class MustCloseIntoObject {
  void test() throws Exception {
    // :: error: required.method.not.called
    Object o = new Socket("", 0);
  }
}
