import java.util.Optional;
import org.checkerframework.checker.optional.qual.*;
import org.checkerframework.dataflow.qual.*;

@SuppressWarnings({"optional:field", "optional:parameter"})
public class Shadowing {

  public Optional<String> f;

  public Optional<String> g;

  class Sub extends Shadowing {
    Optional<String> f;

    void foo(@Present Optional<String> present) {
      super.f = present;
      @Present Optional<String> ok = super.f;
    }

    void bar(@Present Optional<String> present) {
      this.f = present;
      @Present Optional<String> ok = this.f;
    }

    void baz(@Present Optional<String> present) {
      super.g = present;
      @Present Optional<String> ok1 = this.g;
      @Present Optional<String> ok2 = super.g;
    }

    // @RequiresPresent("super.f")
    // void bar() {}
  }
}
