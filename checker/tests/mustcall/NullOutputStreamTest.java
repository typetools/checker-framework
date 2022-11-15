// @below-java11-jdk-skip-test   OutputStream.nullOutputStream() was introduced in JDK 11.

import java.io.OutputStream;
import org.checkerframework.checker.mustcall.qual.MustCall;

class NullOutputStreamTest {

  void m() {
    @MustCall() OutputStream nullOS = OutputStream.nullOutputStream();
  }
}
