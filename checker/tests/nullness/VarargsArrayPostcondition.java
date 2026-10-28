// A postcondition about a varargs parameter, at a call that passes an array to that parameter
// rather than passing varargs arguments.  The array is the argument for the parameter.

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VarargsArrayPostcondition {

  @SafeVarargs
  @EnsuresNonNull("#1")
  static <T> void generic(T @Nullable ... ts) {
    if (ts == null) {
      throw new Error();
    }
  }

  class Generic<T> {
    @SafeVarargs
    @EnsuresNonNull("#1")
    Generic(T @Nullable ... ts) {
      if (ts == null) {
        throw new Error();
      }
    }
  }

  void method(String @Nullable [][] arrays) {
    generic(arrays);
    arrays.toString();
  }

  void constructor(String @Nullable [][] arrays) {
    new Generic<String[]>(arrays);
    arrays.toString();
  }

  void anonymous(String @Nullable [][] arrays) {
    new Generic<String[]>(arrays) {};
    arrays.toString();
  }

  void anonymousEnclosingExpression(VarargsArrayPostcondition outer, String @Nullable [][] arrays) {
    outer.new Generic<String[]>(arrays) {};
    arrays.toString();
  }

  void varargs(@Nullable String s) {
    generic(s, s);
    new Generic<@Nullable String>(s, s);
    new Generic<@Nullable String>(s, s) {};
  }
}
