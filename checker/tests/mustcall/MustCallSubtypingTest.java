// Test case for https://github.com/typetools/checker-framework/issues/5760 .

// @skip-test until the bug is fixed

import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.MustCallUnknown;

public class MustCallSubtypingTest {

  @MustCall({"toString"}) String foo(@MustCall({"hashCode"}) String arg) {
    // :: (return)
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
    // :: (argument)
    requiresMustCallEmptyObject(mcHashCode);
    // :: (argument)
    requiresMustCallEmptyObject(mcToString);
    requiresMustCallEmptyObject(mcEmpty);
    // :: (argument)
    requiresMustCallEmptyObject(mcUnknown);

    // :: (argument)
    requiresMustCallEmptyString(mcHashCode);
    // :: (argument)
    requiresMustCallEmptyString(mcToString);
    requiresMustCallEmptyString(mcEmpty);
    // :: (argument)
    requiresMustCallEmptyString(mcUnknown);

    requiresMustCallHashCodeObject(i);
    requiresMustCallHashCodeObject(ia);
    requiresMustCallHashCodeObject(mcHashCode);
    // :: (argument)
    requiresMustCallHashCodeObject(mcToString);
    requiresMustCallHashCodeObject(mcEmpty);
    // :: (argument)
    requiresMustCallHashCodeObject(mcUnknown);

    requiresMustCallHashCodeString(mcHashCode);
    // :: (argument)
    requiresMustCallHashCodeString(mcToString);
    requiresMustCallHashCodeString(mcEmpty);
    // :: (argument)
    requiresMustCallHashCodeString(mcUnknown);

    requiresMustCallToStringObject(i);
    requiresMustCallToStringObject(ia);
    // :: (argument)
    requiresMustCallToStringObject(mcHashCode);
    requiresMustCallToStringObject(mcToString);
    requiresMustCallToStringObject(mcEmpty);
    // :: (argument)
    requiresMustCallToStringObject(mcUnknown);

    // :: (argument)
    requiresMustCallToStringString(mcHashCode);
    requiresMustCallToStringString(mcToString);
    requiresMustCallToStringString(mcEmpty);
    // :: (argument)
    requiresMustCallToStringString(mcUnknown);

    requiresMustCallUnknownObject(i);
    requiresMustCallUnknownObject(ia);
    // :: (argument)
    requiresMustCallUnknownObject(mcHashCode);
    // :: (argument)
    requiresMustCallUnknownObject(mcToString);
    // :: (argument)
    requiresMustCallUnknownObject(mcEmpty);
    requiresMustCallUnknownObject(mcUnknown);

    // :: (argument)
    requiresMustCallUnknownString(mcHashCode);
    // :: (argument)
    requiresMustCallUnknownString(mcToString);
    // :: (argument)
    requiresMustCallUnknownString(mcEmpty);
    requiresMustCallUnknownString(mcUnknown);
  }
}
