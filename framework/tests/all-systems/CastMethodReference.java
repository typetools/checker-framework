import java.util.function.Function;

// JavaParser does not parse a cast of a method reference correctly:
// https://github.com/javaparser/javaparser/issues/3855
@SuppressWarnings("all") // check for crashes
public class CastMethodReference {
  Object simpleType() {
    return (Runnable) this::hashCode;
  }

  Object parameterizedType() {
    return (Function<String, Integer>) String::length;
  }

  Object parenthesized() {
    return (Function<String, Integer>) (String::length);
  }
}
