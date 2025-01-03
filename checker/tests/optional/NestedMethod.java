import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nonempty.qual.*;
import org.checkerframework.checker.optional.qual.*;

class NestedMethod {

  interface MyRunnable {
    void run();
  }

  void Foo() {
    List<String> foo = new ArrayList<>();
    @NonEmpty List<String> nonEmpty = List.of("A");

    MyRunnable r =
        () -> {
          foo.stream()
              .map(String::length)
              .max(Integer::compare)
              // :: error: (method.invocation)
              .get();
        };
    r.run();

    MyRunnable r2 =
        () -> {
          nonEmpty.stream().map(String::length).max(Integer::compare).get(); // OK
        };
    r2.run();
  }
}
