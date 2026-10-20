// Test case for https://github.com/typetools/checker-framework/issues/8169

import java.util.List;

@SuppressWarnings("all") // Just check for crashes.
public class Issue8169 {
  interface SerializableFunction<InputT, OutputT> {
    OutputT apply(InputT input);
  }

  static <F> List<F> transform(List<F> from) {
    throw new UnsupportedOperationException();
  }

  // The lambda's target type is the raw type `SerializableFunction`, so its function type is the
  // erasure `Object apply(Object)`.  The lambda body is applicable only by unchecked conversion,
  // so its type is the raw type `List`, which must be checked against `Object`.
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static SerializableFunction blockLambda() {
    return list -> {
      if (list == null) {
        throw new AssertionError();
      }
      return transform((List) list);
    };
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static SerializableFunction expressionLambda(List raw) {
    return list -> transform(raw);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void otherTargetContexts(List raw) {
    SerializableFunction inVariable = list -> transform(raw);
    Object inCast = (SerializableFunction) list -> transform(raw);
    use(list -> transform(raw));
  }

  @SuppressWarnings("rawtypes")
  private static void use(SerializableFunction f) {}

  static class Box<T> {
    <U> void consume(SerializableFunction<U, U> fn) {}
  }

  // `consume`'s parameter type is erased because `rawBox` is raw, so the lambda's target type is
  // the raw type `SerializableFunction` and its parameter's type is the erasure of `InputT`.
  // Type argument inference must agree with that, or a `lambda.param` error is issued for a
  // parameter type that the programmer never wrote.
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void rawReceiver(Box rawBox) {
    rawBox.consume(x -> x);
  }
}
