import java.util.Optional;

@SuppressWarnings({
  "optional.field",
  "optional.parameter",
})
public class DisjointRangeTest {

  Optional<String> baz() {
    throw new Error();
  }

  void foo() {
    {
      Optional<String> opt = Optional.of("Hello");
      opt.get(); // OK
    }
    {
      Optional<String> opt = baz();
      // :: error: (method.invocation)
      opt.get();
    }
  }
}
