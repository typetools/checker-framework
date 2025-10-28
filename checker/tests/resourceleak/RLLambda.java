import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.checkerframework.checker.builder.qual.CalledMethods;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCall;

public class RLLambda {
  @InheritableMustCall("a")
  class Foo {
    void a() {}

    Foo b() {
      return this;
    }

    void c(@CalledMethods("a") Foo this) {}
  }

  Foo makeFoo() {
    return new Foo();
  }

  void innerfunc3() {

    Foo f = makeFoo();
    f.a();
    Function<@MustCall Foo, @CalledMethods("a") @MustCall Foo> innerfunc =
        st -> {
          // :: error: (required.method.not.called)
          Foo fn1 = new Foo();
          Foo fn2 = makeFoo();
          fn2.a();
          // The need for this cast is undesirable, but is a consequence of our approach
          // to generic types. In this case, this cast is clearly safe (a() has already
          // been called, so the obligation is satisfied on the returned value, as
          // intended).
          // :: warning: cast.unsafe
          return ((@MustCall Foo) fn2);
        };

    innerfunc.apply(f);
  }

  private void prepJdkFromFile(Path root) {
    try (Stream<Path> walk = Files.walk(root)) {
      Predicate<Path> pathPredicate = p -> false;
    } catch (IOException e) {
      throw new MyException("prepJdkFromFile", e);
    }
  }

  public static class MyException extends RuntimeException {
    public MyException(String s, Exception e) {}
  }
}
