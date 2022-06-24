// A test that the checker is sound in the presence of instance initializer blocks.

import java.net.Socket;
import org.checkerframework.checker.mustcall.qual.*;

class InstanceInitializer {
  // :: error: required.method.not.called
  private @Owning Socket s;

  private final int DEFAULT_PORT = 5;
  private final String DEFAULT_ADDR = "localhost";

  {
    try {
      // :: error: required.method.not.called
      s = new Socket(DEFAULT_ADDR, DEFAULT_PORT);
    } catch (Exception e) {
    }
  }

  {
    try {
      // :: error: required.method.not.called
      s = new Socket(DEFAULT_ADDR, DEFAULT_PORT);
    } catch (Exception e) {
    }
  }

  {
    try {
      // :: error: required.method.not.called
      Socket s1 = new Socket(DEFAULT_ADDR, DEFAULT_PORT);
    } catch (Exception e) {
    }
  }

  {
    Socket s1 = null;
    try {
      s1 = new Socket(DEFAULT_ADDR, DEFAULT_PORT);
    } catch (Exception e) {
    }
    s1.close();
  }

  public InstanceInitializer() throws Exception {
    // :: error: required.method.not.called
    s = new Socket(DEFAULT_ADDR, DEFAULT_PORT);
  }
}
