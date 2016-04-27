
// see also the test for Issue450
// Test file for lambda syntax

import java.lang.Runnable;
import java.lang.String;
import java.lang.Thread;

interface Supplier<R> {
    R supply();
}
interface Function<T, R> {
    R apply(T t);
}
interface Consumer<T> {
    void consume(T t);
}
interface BiFunction<T, U, R> {
    R apply(T t, U u);
}

interface Noop {
    void noop();
}

class Lambda {

    public static void consumeStr(String str) {}

    Lambda(Consumer<String> consumer) {
        consumer.consume("hello");
    }


    Noop f1 = () -> {};                // No parameters; result is void
    Supplier<Integer> f2 = () -> 42;                // No parameters, expression body
//    Supplier<Void> f3 = () -> null;              // No parameters, expression body
    Supplier<Integer> f4 = () -> { return 42; };    // No parameters, block body with return
    Noop f5 = () -> { System.gc(); };  // No parameters, void block body

    Supplier<Integer> f6 = () -> {                 // Complex block body with returns
      if (true) return 12;
      else {
        int result = 15;
        for (int i = 1; i < 10; i++)
          result *= i;
        Consumer<String> consumer = result > 100 ? Lambda::consumeStr : Lambda::consumeStr; // conditional expression
        return result;
      }
    };

    Function<Integer, Integer> f7 = (Integer x) -> x+1;              // Single declared-type parameter
    Function<Integer, Integer> f9 = (Integer x) -> { return (Integer) x+1; };  // Single declared-type parameter
    Function<Integer, Integer> f10 = (x) -> x+1;                  // Single inferred-type parameter
    Function<Integer, Integer> f11 = x -> x+1;                    // Parentheses optional for
                                // single inferred-type parameter

    Function<String, Integer> f12 = (String s) -> s.length() ;     // Single declared-type parameter
    Consumer<Thread> f13 = (Thread t) -> { t.start(); };  // Single declared-type parameter
    Consumer<String> f14 = s -> s.length();               // Single inferred-type parameter
    Consumer<Thread> f15 = t -> { t.start(); };           // Single inferred-type parameter

    BiFunction<Integer, Integer, Integer> f16 = (Integer x, final Integer y) -> x+y;  // Multiple declared-type parameters
    BiFunction<String, String, String> f18 = (x, y) -> x+y;          // Multiple inferred-type parameters
}
