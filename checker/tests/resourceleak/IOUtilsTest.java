import org.checkerframework.checker.mustcall.qual.*;
import java.io.*;

class IOUtilsTest {
  static void test1(@Owning InputStream inputStream) {
    org.apache.commons.io.IOUtils.closeQuietly(inputStream);
  }

  static void test2(@Owning InputStream inputStream) throws IOException {
    try {
      InputStream other = org.apache.commons.io.IOUtils.toBufferedInputStream(inputStream);
    } finally {
      inputStream.close();
    }
  }
}
