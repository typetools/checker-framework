import org.checkerframework.checker.nullness.qual.*;

interface FunctionBC<T extends @Nullable Object, R> {
  R apply(T t);
}

public class FromByteCode {

  FunctionBC<String, String> f1 = String::toString;

  // Make sure there aren't any issues generating an error with a method from byte code
  // :: error: (methodref.param.invalid)
  FunctionBC<@Nullable String, String> f2 = String::new;
}
