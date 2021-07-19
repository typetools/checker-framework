import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.interning.qual.*;

/*
 * TODO: Make diamond cleverer:
 *     List<@Interned String> sl = new ArrayList<>();
 * currently is interpreted as
 *     List<@Interned String> sl = new ArrayList<String>();
 * and then the assignment fails.
 */
public class Raw3 {

  // We would like behavior that is as similar as possible between the
  // versions with no raw types and those with raw types.

  // no raw types
  List<String> foo1() {
    List<String> sl = new ArrayList<>();
    return (List<String>) sl;
  }

  // with raw types
  List<String> foo2() {
    List<String> sl = new ArrayList<>();
    // :: warning: [unchecked] unchecked conversion
    return (List) sl;
  }

  // no raw types
  List<String> foo3() {
    List<@Interned String> sl = new ArrayList<>();
    // :: error: (return)
    return (List<@Interned String>) sl;
  }

  // with raw types
  List<String> foo4() {
    List<@Interned String> sl = new ArrayList<>();
    // :: warning: [unchecked] unchecked conversion
    return (List) sl;
  }

  // no raw types
  List<@Interned String> foo5() {
    List<String> sl = new ArrayList<>();
    // :: error: (return)
    return (List<String>) sl;
  }

  // with raw types
  List<@Interned String> foo6() {
    List<String> sl = new ArrayList<>();
    // :: warning: [unchecked] unchecked conversion
    return (List) sl;
  }

  class TestList<T> {
    List<String> bar1() {
      List<String> sl = new ArrayList<>();
      return (List<String>) sl;
    }

    List<String> bar2() {
      List<String> sl = new ArrayList<>();
      // :: warning: [unchecked] unchecked conversion
      return (List) sl;
    }

    List<String> bar3(List<String> sl) {
      // :: warning: [unchecked] unchecked conversion
      return (List) sl;
    }

    class DuoList<S, T> extends ArrayList<S> {}

    List<String> bar4(List<String> sl) {
      // This line was previously failing because we couldn't adequately infer the type of DuoList
      // as a List; it works now, though the future checking of rawtypes may be more strict.
      // :: warning: [unchecked] unchecked conversion
      return (DuoList) sl;
    }
  }
}
