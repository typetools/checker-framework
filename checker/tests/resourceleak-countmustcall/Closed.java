// Test input for ResourceLeakCountMustCallTest.

import java.io.FileInputStream;
import java.io.IOException;

class Closed {
  void close(String name) throws IOException {
    FileInputStream s = new FileInputStream(name);
    s.close();
  }

  void closeInLoop(String[] names) throws IOException {
    for (String name : names) {
      FileInputStream s = new FileInputStream(name);
      try {
        s.read();
      } finally {
        s.close();
      }
    }
  }
}
