import java.io.IOException;
import java.io.PrintStream;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.Owning;

public class TypeProcessError {

  @SuppressWarnings("required.method.not.called")
  private static @Owning @MustCall("close") PrintStream trace_file_static;

  @SuppressWarnings("missing.creates.mustcall.for")
  static void m_static() throws IOException {
    trace_file_static.close();
    trace_file_static = new PrintStream("filename.txt");
  }
}
