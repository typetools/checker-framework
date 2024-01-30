import java.util.List;

class Comparisons {

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

  /**** Tests for GT ****/
  void t1(List<String> strs) {
    if (strs.size() > 0) {
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
  }

  void t2(List<String> strs) {
    if (strs.size() > -1) {
      // :: error: (method.invocation)
      strs.iterator().next();
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

  /**** Tests for LT ****/
  void t5(List<String> strs) {
    if (strs.size() < 10) {
      // :: error: (method.invocation)
      strs.iterator().next();
    } else if (strs.size() < 1) {
      // :: error: (method.invocation)
      strs.iterator().next();
    }
  }

  void t6(List<String> strs) {
    if (0 < strs.size()) {
      strs.iterator().next();
    }
    if (10 < strs.size()) {
      strs.iterator().next();
    }
    if (-1 < strs.size()) {
      // :: error: (method.invocation)
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
    if (0 <= strs.size()) {
      // :: error: (method.invocation)
      strs.iterator().next();
    } else if (1 <= strs.size()) {
      strs.iterator().next();
    } else if (10 <= strs.size()) {
      strs.iterator().next();
    }
    if (0 <= strs.size()) {
      // :: error: (method.invocation)
      strs.iterator().next();
    } else {
      strs.iterator().next(); // OK, since strs must be non-empty
    }
  }
}
