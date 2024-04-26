import java.util.Optional;
import java.util.function.Function;

class OptionalClosureTest {

  @SuppressWarnings("optional:field") // We don't care, not the point of this test
  public Optional<String> opt;

  public Function<String, String> test() {
    if (opt.isPresent()) {
      // We *should* issue an error here. It's no good that opt is @Present here; it might be
      // @MaybePresent at the time of invocation, for which get() is illegal.
      // :: error: (method.invocation)
      return str -> opt.get();
    }
    return str -> "Hello";
  }

  @SuppressWarnings("optional:parameter") // We don't care, not the point of this test
  public void setOpt(Optional<String> opt) {}
}
