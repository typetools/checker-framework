import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// A method invocation whose return type is an inference variable that has a raw lower bound, and
// whose target type is a parameterization of the same generic class.  This is the second
// alternative of JLS 18.5.2.1, which applies whether or not the target type is
// wildcard-parameterized.  The inference variable is resolved before the target type is taken into
// account.
public class RawTypeTarget {
  static <T> T id(T t) {
    throw new AssertionError();
  }

  static <T> T two(T a, T b) {
    throw new AssertionError();
  }

  static List rawList() {
    throw new AssertionError();
  }

  static ArrayList rawArrayList() {
    throw new AssertionError();
  }

  static Map rawMap() {
    throw new AssertionError();
  }

  // In each assignment below, type systems may issue an error because of a mismatch between the
  // type arguments of the raw type and of the target type.  See RawTypeAssignment.java.

  // The target type is not wildcard-parameterized.
  @SuppressWarnings("assignment")
  void nonWildcardTarget() {
    // :: warning: [unchecked] unchecked conversion
    List<String> l1 = id(rawList());
    // :: warning: [unchecked] unchecked conversion
    List<String> l2 = id(rawArrayList());
    // :: warning: [unchecked] unchecked conversion
    Map<String, Integer> m = id(rawMap());
    // :: warning: [unchecked] unchecked conversion
    List<String> l3 = two(rawList(), rawArrayList());
  }

  // The target type is wildcard-parameterized.
  @SuppressWarnings("assignment")
  void wildcardTarget() {
    // :: warning: [unchecked] unchecked conversion
    List<? extends String> l = id(rawList());
    // :: warning: [unchecked] unchecked conversion
    Map<String, ?> m = id(rawMap());
  }

  // The inference variable has no raw bound, so the second alternative does not apply.
  void noRawBound(List<String> arg) {
    List<String> l = id(arg);
  }
}
