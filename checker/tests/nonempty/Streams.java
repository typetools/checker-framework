import java.util.List;
import java.util.stream.Stream;
import org.checkerframework.checker.nonempty.qual.NonEmpty;

class Streams {

  void testSingletonStreamCreation() {
    @NonEmpty Stream<Integer> strm = Stream.of(1); // OK
  }

  void testStreamAnyMatch(Stream<String> strStream) {
    if (strStream.anyMatch(str -> str.length() > 10)) {
      @NonEmpty Stream<String> neStream = strStream;
    } else {
      // :: error: (assignment)
      @NonEmpty Stream<String> err = strStream;
    }
  }

  void testStreamAllMatch(Stream<String> strStream) {
    if (strStream.allMatch(str -> str.length() > 10)) {
      @NonEmpty Stream<String> neStream = strStream;
    } else {
      // :: error: (assignment)
      @NonEmpty Stream<String> err = strStream;
    }
  }

  void testMapNonEmptyStream(@NonEmpty List<String> strs) {
    @NonEmpty Stream<Integer> lens = strs.stream().map(str -> str.length());
  }

  void testMapNonEmptyStream(Stream<String> strs) {
    // :: error: (assignment)
    @NonEmpty Stream<Integer> lens = strs.map(str -> str.length());
  }

  void testNoneMatch(Stream<String> strs) {
    if (strs.noneMatch(str -> str.length() < 10)) {
      // :: error: (assignment)
      @NonEmpty Stream<String> err = strs;
    } else {
      // something matched; meaning that the stream MUST be non-empty
      @NonEmpty Stream<String> nonEmptyStrs = strs;
    }
  }
}
