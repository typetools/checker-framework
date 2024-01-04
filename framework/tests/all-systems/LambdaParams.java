import java.lang.reflect.Method;
import java.util.function.Function;

public class LambdaParams {
  interface Stream<T> {
    <S> Stream<S> map(Function<? super T, ? extends S> mapper);
  }

  static <Z> Z identity(Z p) {
    throw new RuntimeException();
  }

  static <Q> Stream<Q> stream(Q[] array) {
    throw new RuntimeException();
  }

  static void method() {
    Function<? super Method, ? extends Stream<? extends String>> mapper =
        identity(f -> stream(f.getAnnotations()).map(annotation -> ""));
  }
}
