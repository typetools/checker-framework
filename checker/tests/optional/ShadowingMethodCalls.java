import java.util.Optional;
import org.checkerframework.checker.optional.qual.EnsuresPresent;
import org.checkerframework.dataflow.qual.Pure;

@SuppressWarnings({"optional:field", "optional:parameter"})
public class ShadowingMethodCalls {

  Optional<String> f;

  @EnsuresPresent("this.f")
  void setF() {
    this.f = Optional.of("Hello from Parent");
  }

  class Child extends ShadowingMethodCalls {

    Optional<String> f;

    @EnsuresPresent("super.f")
    void setF() {
      super.f = Optional.of("Hello from Child");
    }

    void callThisAndCheckSuper() {
      this.setF();
      super.f.get();
    }

    void callSuperAndCheckSuper() {
      super.setF();
      super.f.get();
    }

    void callSuperAndCheckThis() {
      super.setF();
      // :: error: (method.invocation)
      this.f.get();
    }

    void callThisAndCheckThis() {
      this.setF();
      // :: error: (method.invocation)
      this.f.get();
      super.f.get();
    }
  }

  static class ClassA {

    @Pure
    Optional<String> getOpt() {
      throw new RuntimeException();
    }
  }

  static class ClassB extends ClassA {
    void use() {
      if (super.getOpt().isPresent()) {
        String s = super.getOpt().get();
      }
      if (super.getOpt().isPresent()) {
        String s = this.getOpt().get();
      }
      if (this.getOpt().isPresent()) {
        String s = super.getOpt().get();
      }
      if (this.getOpt().isPresent()) {
        String s = this.getOpt().get();
      }
    }
  }
}
