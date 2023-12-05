import java.util.Optional;

public class IfPresentRefinement {

  @SuppressWarnings("optional.parameter")
  void m1(Optional<String> o) {
    o.ifPresent(s -> o.get());
  }

  @SuppressWarnings("optional.parameter")
  void m2(Optional<String> o) {
    o.ifPresentOrElse(s -> o.get(), () -> {});
  }

  @SuppressWarnings("optional.parameter")
  void m3(Optional<String> o) {
    // :: error: (method.invocation)
    o.ifPresentOrElse(s -> o.get(), () -> o.get());
  }
}
