import org.checkerframework.checker.mustcall.qual.*;

class IOUtilsTest {
  static void foo(@Owning java.io.InputStream inputStream) {
    org.apache.commons.io.IOUtils.closeQuietly(inputStream);
  }
}
