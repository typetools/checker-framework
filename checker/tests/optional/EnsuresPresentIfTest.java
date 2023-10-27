import java.util.Optional;
import org.checkerframework.checker.optional.qual.*;
import org.checkerframework.checker.optional.qual.EnsuresPresentIf;

public class EnsuresPresentIfTest {

  // :: warning: (optional.field)
  private Optional<String> optId = Optional.of("abc");

  public @org.checkerframework.dataflow.qual.Pure Optional<String> getOptId() {
    return Optional.of("abc");
  }

  @EnsuresPresentIf(result = true, expression = "getOptId()")
  public boolean hasPresentId1() {
    return getOptId().isPresent();
  }

  void client() {
    if (hasPresentId1()) {
      optId.get();
    }
    // :: error: (method.invocation)
    optId.get();
  }
}
