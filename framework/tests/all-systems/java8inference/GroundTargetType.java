// Explicitly typed lambdas whose target type is a wildcard-parameterized functional interface.
// These exercise JLS 18.5.3, Functional Interface Parameterization Inference.

import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("all") // Just check for crashes.
public class GroundTargetType {

  interface Bounded<T extends Number> {
    String apply(T t);
  }

  <T> void takesPredicate(Predicate<? super T> p) {}

  <T> void takesFunction(Function<? super T, ? extends String> fn) {}

  <U extends Number> void takesBounded(Bounded<? extends U> b) {}

  void m() {
    // The lambda is a Predicate<Number>, which is a subtype of Predicate<? super Integer> but
    // not of Predicate<Integer>.  This is the example in the JLS 18.5.3 discussion.
    takesPredicate((Number n) -> n.equals(23));
    takesFunction((Number n) -> n.toString());
    // The type parameter of Bounded has a bound other than Object, so the well-formedness test
    // of 18.5.3 is not trivial.
    takesBounded((Integer i) -> i.toString());
  }
}
