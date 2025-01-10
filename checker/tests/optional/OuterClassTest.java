import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nonempty.qual.*;
import org.checkerframework.checker.optional.qual.*;

class OuterClassTest {

  private @NonEmpty List<String> strs = List.of("1");
  private List<String> emptyStrs = new ArrayList<>();

  class Inner {

    void validGet() {
      strs.stream().map(String::length).max(Integer::compare).get(); // OK
    }

    class InnerInner {

      void invalidGet() {
        emptyStrs.stream()
            .map(String::length)
            .max(Integer::compare)
            // :: error: (method.invocation)
            .get();
      }

      void validGet() {
        strs.stream().map(String::length).max(Integer::compare).get(); // OK
      }
    }
  }
}
