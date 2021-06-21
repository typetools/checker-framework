// A test that when a must-call class with MustCallAlias methods
// is extended, those methods can be used without false positives.

import java.net.Socket;

class MCAWithThis extends Socket {
  public MCAWithThis() {
    super();
  }

  public void test() throws Exception {
    this.getInputStream();
  }

  public void test2() throws Exception {
    getInputStream();
  }
}
