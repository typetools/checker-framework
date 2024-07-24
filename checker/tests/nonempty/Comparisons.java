import java.util.Arrays;
import java.util.List;
import org.checkerframework.checker.nonempty.qual.NonEmpty;

class Comparisons {

  /**** Tests for EQ ****/
  void testEqZeroWithReturn(List<String> strs) {
    if (strs.size() == 0) {
      // :: error: (method.invocation)
      strs.iterator().next();
      return;
    }
    strs.iterator().next(); // OK
  }

  void testEqZeroFallthrough(List<String> strs) {
    if (strs.size() == 0) {
      // :: error: (method.invocation)
      strs.iterator().next();
    }
    // :: error: (method.invocation)
    strs.iterator().next();
  }

  void testEqNonZero(List<String> strs) {
    if (1 == strs.size()) {
      strs.iterator().next();
    } else {
      // :: error: (method.invocation)
      strs.iterator().next();
    }
  }

  void testImplicitNonZero(List<String> strs1, List<String> strs2) {
    if (strs1.isEmpty()) {
      return;
    }
    if (strs1.size() == strs2.size()) {
      @NonEmpty List<String> strs3 = strs2; // OK
    }
    // :: error: (assignment)
    @NonEmpty List<String> strs4 = strs2;
  }

  void testImplicitNonZero2(List<String> strs2) {
    if (getNonEmptyList().size() == strs2.size()) {
      @NonEmpty List<String> strs3 = strs2; // OK
    }
  }

  @NonEmpty
  List<String> getNonEmptyList() {
    return Arrays.asList(new String[] {""});
  }

  void testEqualIndexOfRefinement(List<Object> objs, Object obj) {
    if (objs.indexOf(obj) == -1) {
      // :: error: (assignment)
      @NonEmpty List<Object> objs2 = objs;
    } else {
      objs.iterator().next();
    }
  }

  /**** Tests for NE ****/
  void t0(List<String> strs) {
    if (strs.size() != 0) {
      strs.iterator().next();
    }
    if (0 != strs.size()) {
      strs.iterator().next();
    }
    if (1 != strs.size()) {
      // :: error: (method.invocation)
      strs.iterator().next();
    }
  }

  void testNotEqualsRefineElse(List<String> strs1, List<String> strs2) {
    if (strs1.size() <= 0) {
      return;
    }
    if (strs1.size() != strs2.size()) {
      // :: error: (assignment)
      @NonEmpty List<String> strs3 = strs2;
    } else {
      @NonEmpty List<String> strs4 = strs1;
      @NonEmpty List<String> strs5 = strs2;
    }
  }

  void testNotEqualsRefineIndexOf(List<Object> objs, Object obj) {
    if (objs.indexOf(obj) != -1) {
      @NonEmpty List<Object> objs2 = objs;
    } else {
      // :: error: (method.invocation)
      objs.iterator().next();
    }
    if (-1 != objs.indexOf(obj)) {
      @NonEmpty List<Object> objs2 = objs;
    } else {
      // :: error: (method.invocation)
      objs.iterator().next();
    }
  }

  /**** Tests for GT ****/
  void t1(List<String> strs) {
    if (strs.size() > 10) {
      strs.iterator().next();
    } else if (0 > strs.size()) {
      // :: error: (method.invocation)
      strs.iterator().next();
    } else if (100 > strs.size()) {
      // :: error: (method.invocation)
      strs.iterator().next();
    }
    if (strs.size() > 0) {
      strs.iterator().next();
    } else {
      // :: error: (method.invocation)
      strs.iterator().next();
    }

    if (0 > strs.size()) {
      // :: error: (method.invocation)
      strs.iterator().next();
    } else {
      // :: error: (method.invocation)
      strs.iterator().next();
    }
  }

  void t2(List<String> strs) {
    if (strs.size() > -1) {
      // :: error: (method.invocation)
      strs.iterator().next();
    }
  }

  void testRefineIndexOfGT(List<Object> objs, Object obj) {
    if (objs.indexOf(obj) > -1) {
      @NonEmpty List<Object> objs2 = objs;
    } else {
      // :: error: (method.invocation)
      objs.iterator().next();
    }
  }

  /**** Tests for GTE ****/
  void t3(List<String> strs) {
    if (strs.size() >= 0) {
      // :: error: (method.invocation)
      strs.iterator().next();
    } else if (strs.size() >= 1) {
      strs.iterator().next();
    }
  }

  void t4(List<String> strs) {
    if (0 >= strs.size()) {
      // :: error: (method.invocation)
      strs.iterator().next();
    }
  }

  void testRefineGTEIndexOf(List<String> strs, String s) {
    if (strs.indexOf(s) >= 0) {
      strs.iterator().next();
    } else {
      // :: error: (assignment)
      @NonEmpty List<String> strs2 = strs;
    }
  }

  /**** Tests for LT ****/
  void t5(List<String> strs) {
    if (strs.size() < 10) {
      // :: error: (method.invocation)
      strs.iterator().next();
    }
    if (strs.size() < 1) {
      // :: error: (method.invocation)
      strs.iterator().next();
    } else {
      strs.iterator().next(); // OK
    }
  }

  void t6(List<String> strs) {
    if (0 < strs.size()) {
      strs.iterator().next(); // Equiv. to strs.size() > 0
    } else {
      // :: error: (method.invocation)
      strs.iterator().next(); // Equiv. to strs.size() <= 0
    }

    if (strs.size() < 10) {
      // Doesn't tell us a useful fact
      // :: error: (method.invocation)
      strs.iterator().next();
    } else {
      strs.iterator().next();
    }
  }

  /**** Tests for LTE ****/
  void t7(List<String> strs) {
    if (strs.size() <= 2) {
      // :: error: (method.invocation)
      strs.iterator().next();
    }
    if (strs.size() <= 0) {
      // :: error: (method.invocation)
      strs.iterator().next();
    } else {
      strs.iterator().next(); // OK, since strs must be non-empty
    }
  }

  void t8(List<String> strs) {
    if (1 <= strs.size()) {
      strs.iterator().next();
    } else {
      // :: error: (method.invocation)
      strs.iterator().next();
    }

    if (0 <= strs.size()) {
      // :: error: (method.invocation)
      strs.iterator().next();
    } else {
      // :: error: (method.invocation)
      strs.iterator().next();
    }
  }
}
