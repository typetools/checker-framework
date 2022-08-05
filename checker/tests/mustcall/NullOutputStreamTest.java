import java.io.OutputStream;
import org.checkerframework.checker.mustcall.qual.MustCall;

class NullOutputStreamTest {

  void m() {
    @MustCall() OutputStream nullOS = OutputStream.nullOutputStream();
  }
}
