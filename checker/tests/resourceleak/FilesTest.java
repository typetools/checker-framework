import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.checkerframework.checker.mustcall.qual.MustCall;

public class FilesTest {

  void bad(Path p) throws IOException {
    // error: [required.method.not.called]
    Stream<Path> s = Files.list(p);
  }

  void good(Path p) throws IOException {
    // TODO: Programmers should not be required to write this `@MustCall("close")`.
    try (@MustCall("close") Stream<Path> s = Files.list(p)) {
      // empty body
    }
  }
}
