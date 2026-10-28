// Test case for https://github.com/typetools/checker-framework/issues/5760 .

// @skip-test until the bug is fixed

import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.MustCallUnknown;

public class MustCallSubtypingTest {

  @MustCall({"toString"}) String foo(@MustCall({"hashCode"}) String arg) {
    // :: error: [return]
    return arg;
  }

  @MustCall({}) String mcEmpty;

  @MustCall({"hashCode"}) String mcHashCode;

  @MustCall({"toString"}) String mcToString;

  @MustCallUnknown String mcUnknown;

  void clientSetMcEmpty() {
    mcEmpty = mcHashCode;
    mcEmpty = mcToString;
    mcEmpty = mcUnknown;
  }

  void clientSetMcHashCode() {
    mcHashCode = mcEmpty;
    mcHashCode = mcToString;
    mcHashCode = mcUnknown;
  }

  void clientSetMcToString() {
    mcToString = mcEmpty;
    mcToString = mcHashCode;
    mcToString = mcUnknown;
  }

  void clientSetMcUnknown() {
    mcUnknown = mcEmpty;
    mcUnknown = mcHashCode;
    mcUnknown = mcToString;
  }

  void requiresMustCallEmptyObject(@MustCall({}) Object o) {}

  void requiresMustCallHashCodeObject(@MustCall({"hashCode"}) Object o) {}

  void requiresMustCallToStringObject(@MustCall({"toString"}) Object o) {}

  void requiresMustCallUnknownObject(@MustCallUnknown Object o) {}

  void requiresMustCallEmptyString(@MustCall({}) String s) {}

  void requiresMustCallHashCodeString(@MustCall({"hashCode"}) String s) {}

  void requiresMustCallToStringString(@MustCall({"toString"}) String s) {}

  void requiresMustCallUnknownString(@MustCallUnknown String s) {}

  void client(Integer i, Integer[] ia) {
    requiresMustCallEmptyObject(i);
    requiresMustCallEmptyObject(ia);
    // :: error: [argument]
    requiresMustCallEmptyObject(mcHashCode);
    // :: error: [argument]
    requiresMustCallEmptyObject(mcToString);
    requiresMustCallEmptyObject(mcEmpty);
    // :: error: [argument]
    requiresMustCallEmptyObject(mcUnknown);

    // :: error: [argument]
    requiresMustCallEmptyString(mcHashCode);
    // :: error: [argument]
    requiresMustCallEmptyString(mcToString);
    requiresMustCallEmptyString(mcEmpty);
    // :: error: [argument]
    requiresMustCallEmptyString(mcUnknown);

    requiresMustCallHashCodeObject(i);
    requiresMustCallHashCodeObject(ia);
    requiresMustCallHashCodeObject(mcHashCode);
    // :: error: [argument]
    requiresMustCallHashCodeObject(mcToString);
    requiresMustCallHashCodeObject(mcEmpty);
    // :: error: [argument]
    requiresMustCallHashCodeObject(mcUnknown);

    requiresMustCallHashCodeString(mcHashCode);
    // :: error: [argument]
    requiresMustCallHashCodeString(mcToString);
    requiresMustCallHashCodeString(mcEmpty);
    // :: error: [argument]
    requiresMustCallHashCodeString(mcUnknown);

    requiresMustCallToStringObject(i);
    requiresMustCallToStringObject(ia);
    // :: error: [argument]
    requiresMustCallToStringObject(mcHashCode);
    requiresMustCallToStringObject(mcToString);
    requiresMustCallToStringObject(mcEmpty);
    // :: error: [argument]
    requiresMustCallToStringObject(mcUnknown);

    // :: error: [argument]
    requiresMustCallToStringString(mcHashCode);
    requiresMustCallToStringString(mcToString);
    requiresMustCallToStringString(mcEmpty);
    // :: error: [argument]
    requiresMustCallToStringString(mcUnknown);

    requiresMustCallUnknownObject(i);
    requiresMustCallUnknownObject(ia);
    // :: error: [argument]
    requiresMustCallUnknownObject(mcHashCode);
    // :: error: [argument]
    requiresMustCallUnknownObject(mcToString);
    // :: error: [argument]
    requiresMustCallUnknownObject(mcEmpty);
    requiresMustCallUnknownObject(mcUnknown);

    // :: error: [argument]
    requiresMustCallUnknownString(mcHashCode);
    // :: error: [argument]
    requiresMustCallUnknownString(mcToString);
    // :: error: [argument]
    requiresMustCallUnknownString(mcEmpty);
    requiresMustCallUnknownString(mcUnknown);
  }
}
