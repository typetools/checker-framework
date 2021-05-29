// Test case for issue #1214:
// https://github.com/typetools/checker-framework/issues/1214

public class Issue1214 {
  static String ng1() {
    String s = "not null";
    try {
      int data = 50 / 0;
    } catch (Exception e) {
      s = null;
    }
    // :: error: (return)
    return s;
  }

  static String ng2(int x) {
    String s = "not null";
    try {
      short data = (short) (50 / x);
    } catch (Exception e) {
      try {
        s = null;
      } catch (Exception ee) {
      }
    }
    // :: error: (return)
    return s;
  }

  static String ng3() {
    String s = "not null";
    try {
      int data = 50 % 0;
    } catch (Exception e) {
      try {
        // some statements...
      } catch (Exception ee) {
      } finally {
        s = null;
      }
    }
    // :: error: (return)
    return s;
  }

  static String ng4(int data) {
    String s = "not null";
    try {
      data /= 0;
    } catch (Exception e) {
      s = null;
    }
    // :: error: (return)
    return s;
  }

  static String ng5(short data) {
    String s = "not null";
    try {
      data /= 0;
    } catch (Exception e) {
      try {
        s = null;
      } catch (Exception ee) {
      }
    }
    // :: error: (return)
    return s;
  }

  static String ng6(int data) {
    String s = "not null";
    try {
      data %= 0;
    } catch (Exception e) {
      try {
        // some statements...
      } catch (Exception ee) {
      } finally {
        s = null;
      }
    }
    // :: error: (return)
    return s;
  }

  static String ok1() {
    String s = "not null";
    try {
      double data = 50 / 0.0;
    } catch (Exception e) {
      s = null;
    }
    return s;
  }

  static String ok2() {
    String s = "not null";
    try {
      double data = 50 % 0.0;
    } catch (Exception e) {
      s = null;
    }
    return s;
  }

  static String ok3(double data) {
    String s = "not null";
    try {
      data /= 0;
    } catch (Exception e) {
      s = null;
    }
    return s;
  }

  static String ok4(float data) {
    String s = "not null";
    try {
      data %= 0;
    } catch (Exception e) {
      s = null;
    }
    return s;
  }
}
