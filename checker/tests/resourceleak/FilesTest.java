import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class FilesTest {

    void bad(Path p) throws IOException {
        // :: error: (required.method.not.called)
        Stream<Path> s = Files.list(p);
    }

    void good(Path p) throws IOException {
        try (Stream<Path> s = Files.list(p)) {
            // empty body
        }
    }
}
