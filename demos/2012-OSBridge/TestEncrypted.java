public class TestEncrypted {
  void send(@Encrypted String p) {
    // ...
  }

  void test() {
    send("bad");
  }
}
