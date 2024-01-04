import java.util.Optional;

@SuppressWarnings("optional.parameter")
public class IfPresentRefinement {

  void m1(Optional<String> o) {
    o.ifPresent(s -> o.get());
  }

  void m2(Optional<String> o) {
    o.ifPresentOrElse(s -> o.get(), () -> {});
  }

  void m3(Optional<String> o) {
    // :: error: (method.invocation)
    o.ifPresentOrElse(s -> o.get(), () -> o.get());
  }
}
