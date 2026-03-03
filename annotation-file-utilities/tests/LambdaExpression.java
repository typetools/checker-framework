import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;
import java.util.function.ToIntBiFunction;

@Target(ElementType.TYPE_USE)
@interface A {}

@Target(ElementType.TYPE_USE)
@interface B {}

@Target(ElementType.TYPE_USE)
@interface C {}

class LambdaExpression {
  // Single inferred-type parameter
  IntFunction<Integer> f0 = (x) -> x + 1;

  // Parentheses optional for single inferred-type parameter
  IntFunction<Integer> f1 = x -> x + 1;

  // Single declared-type parameter, expression body
  IntFunction<Integer> f2 = (int x) -> x + 1;

  // Single declared-type parameter, block body
  IntFunction<Integer> f3 =
      (int x) -> {
        return x + 1;
      };

  // Multiple declared-type parameters
  IntBinaryOperator f4 = (int x, int y) -> x + y;

  // Generic argument type
  static final ToIntBiFunction<String[], List<? extends CharSequence>> selectCommon =
      (String[] array, List<? extends CharSequence> list) -> {
        int total = 0;
        for (int i = 0; i < array.length; i++) {
          Iterator<? extends CharSequence> iter = list.iterator();
          String str = array[i];
          while (iter.hasNext()) {
            CharSequence seq = iter.next();
            if (seq.toString().equals(str)) {
              ++total;
              iter.remove();
              break;
            }
          }
        }
        return total;
      };

  public static void main(String[] args) {
    String[] ss = {"a", "b"};
    System.out.println(selectCommon.applyAsInt(args, Arrays.asList(ss)));
  }
}
