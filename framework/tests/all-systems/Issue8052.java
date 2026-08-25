import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * A block-bodied lambda that declares a class (anonymous or local) in its body crashed inference
 * with {@code FalseBoundException: False bound for: Constraint: @Tainted Object <: @Tainted
 * Getter<...>}.
 *
 * <p>Reduced from Apache Beam, {@code
 * sdks/java/extensions/arrow/src/main/java/org/apache/beam/sdk/extensions/arrow/ArrowConversion.java}
 * line 371, {@code FieldVectorListValueGetterFactory#create}, which maps over a stream with a block
 * lambda that returns an anonymous {@code FieldValueGetter<Integer, Object>}.
 *
 * <p>{@code TreeUtils.getReturnedExpressions(LambdaExpressionTree)} scans the lambda body for
 * {@code return} statements. It stops at a nested lambda, but not at a nested class declaration, so
 * a {@code return} inside a method of an anonymous or local class declared in the body is collected
 * as if it were a result expression of the lambda itself. The lub of the real result and the bogus
 * one is {@code Object}, which then produces the false bound {@code Object <: Getter<K, V>}.
 *
 * <p>The bogus result expression only has to be reachable from the body, so the nested class need
 * not be the returned expression at all -- {@code doesNotEvenReturnIt} crashes too.
 */
public class Issue8052 {

  interface Getter<K, V> {
    V get(K k);
  }

  /** Marker interface: an anonymous subtype has no method to implement. */
  interface Marker<K, V> {}

  static native boolean cond();

  // The Beam shape: a block lambda returning an anonymous class that implements the interface.
  List<Getter<Integer, Object>> beamShape(List<String> l) {
    return l.stream()
        .map(
            (v) -> {
              if (cond()) {
                return new Getter<Integer, Object>() {
                  @Override
                  public Object get(Integer rowIndex) {
                    return v;
                  }
                };
              } else {
                return new Getter<Integer, Object>() {
                  @Override
                  public Object get(Integer rowIndex) {
                    return v.length();
                  }
                };
              }
            })
        .collect(Collectors.toList());
  }

  // One anonymous class and one return statement is enough.
  List<Getter<Integer, Object>> singleReturn(List<String> l) {
    return l.stream()
        .map(
            (v) -> {
              return new Getter<Integer, Object>() {
                @Override
                public Object get(Integer rowIndex) {
                  return v;
                }
              };
            })
        .collect(Collectors.toList());
  }

  // The returned expression need not be the class that holds the stray return statement.
  List<Marker<Integer, Object>> doesNotEvenReturnIt(List<String> l) {
    return l.stream()
        .map(
            (v) -> {
              Callable<String> unrelated =
                  new Callable<String>() {
                    @Override
                    public String call() {
                      return "x";
                    }
                  };
              return new Marker<Integer, Object>() {};
            })
        .collect(Collectors.toList());
  }

  // A local class in the body has the same effect.
  List<Marker<Integer, Object>> localClass(List<String> l) {
    return l.stream()
        .map(
            (v) -> {
              class Local {
                String s() {
                  return "x";
                }
              }
              return new Marker<Integer, Object>() {};
            })
        .collect(Collectors.toList());
  }

  // Below: variants that do NOT crash, which locate the bug.

  // An expression-bodied lambda does not scan for return statements.
  List<Getter<Integer, Object>> expressionBodiedLambdaOk(List<String> l) {
    return l.stream()
        .map(
            (v) ->
                new Getter<Integer, Object>() {
                  @Override
                  public Object get(Integer rowIndex) {
                    return v;
                  }
                })
        .collect(Collectors.toList());
  }

  // An anonymous class that declares no method contributes no stray return statement.
  List<Marker<Integer, Object>> noMethodInAnonymousClassOk(List<String> l) {
    return l.stream()
        .map(
            (v) -> {
              return new Marker<Integer, Object>() {
                String field = v;
              };
            })
        .collect(Collectors.toList());
  }

  // getReturnedExpressions already stops at a nested lambda.
  List<Marker<Integer, Object>> nestedLambdaOk(List<String> l) {
    return l.stream()
        .map(
            (v) -> {
              Callable<String> unrelated =
                  () -> {
                    return "x";
                  };
              return new Marker<Integer, Object>() {};
            })
        .collect(Collectors.toList());
  }

  // The same anonymous classes outside a lambda are fine.
  Getter<Integer, Object> outsideALambdaOk() {
    if (cond()) {
      return new Getter<Integer, Object>() {
        @Override
        public Object get(Integer rowIndex) {
          return rowIndex;
        }
      };
    } else {
      return new Getter<Integer, Object>() {
        @Override
        public Object get(Integer rowIndex) {
          return rowIndex;
        }
      };
    }
  }
}
