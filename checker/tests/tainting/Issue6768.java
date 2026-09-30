// Test case for https://github.com/typetools/checker-framework/issues/6768

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Issue6768 {
  List<Foo> b(Path c, ClassProvider classProvider) {
    try (Stream<Path> walk = Files.walk(c)) {
      return walk.flatMap(
              g -> {
                return StreamUtils.optionalToStream(classProvider.provide());
              })
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new IllegalArgumentException();
    }
  }

  abstract static class ClassProvider {
    abstract Optional<? extends Foo> provide();
  }

  static class StreamUtils {
    static <M> Stream<M> optionalToStream(Optional<M> n) {
      throw new RuntimeException();
    }
  }

  static class Foo {}
}
