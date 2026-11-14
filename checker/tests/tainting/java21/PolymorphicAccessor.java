// @below-java21-jdk-skip-test

// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// The binding variable of a record pattern has the type that the record component's accessor
// returns when invoked on the matched value, including when the accessor has polymorphic
// qualifiers.

import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class PolymorphicAccessor {

  record StringHolder(String s) {
    @SuppressWarnings("tainting") // the field is @Tainted
    public @PolyTainted String s(@PolyTainted StringHolder this) {
      return s;
    }
  }

  void untainted(@Untainted StringHolder h) {
    @Untainted String fromAccessor = h.s();
    if (h instanceof StringHolder(var x)) {
      @Untainted String fromVar = x;
    }
    if (h instanceof StringHolder(String y)) {
      @Untainted String fromType = y;
    }
  }

  void tainted(@Tainted StringHolder h) {
    // :: error: (assignment)
    @Untainted String fromAccessor = h.s();
    if (h instanceof StringHolder(var x)) {
      // :: error: (assignment)
      @Untainted String fromVar = x;
    }
    if (h instanceof StringHolder(String y)) {
      // :: error: (assignment)
      @Untainted String fromType = y;
    }
  }

  // Inside a polymorphic method, the binding variable is @PolyTainted, as the accessor invocation
  // is.
  @PolyTainted String polymorphicMethod(@PolyTainted StringHolder h) {
    if (h instanceof StringHolder(var x)) {
      return x;
    }
    return h.s();
  }

  record Holder<T>(T t) {
    @SuppressWarnings("tainting") // the field's type is not @PolyTainted
    public @PolyTainted T t(@PolyTainted Holder<T> this) {
      return t;
    }
  }

  void generic(@Untainted Holder<String> h) {
    @Untainted String fromAccessor = h.t();
    if (h instanceof Holder(var x)) {
      @Untainted String fromVar = x;
    }
    if (h instanceof Holder<String>(String y)) {
      @Untainted String fromType = y;
    }
  }

  void genericTainted(@Tainted Holder<@Untainted String> h) {
    // The receiver is @Tainted, so the accessor's @PolyTainted return type is @Tainted, even
    // though the type argument is @Untainted.
    // :: error: (assignment)
    @Untainted String fromAccessor = h.t();
    if (h instanceof Holder(var x)) {
      // :: error: (assignment)
      @Untainted String fromVar = x;
    }
  }
}
