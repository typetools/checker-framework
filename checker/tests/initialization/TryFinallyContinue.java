// Test case for Issue 548:
// https://github.com/typetools/checker-framework/issues/548

public class TryFinallyContinue {
  String testWhile1() {
    String ans = "x";
    while (true) {
      if (true) {
        // :: error: (return.type.incompatible)
        return ans;
      }
      if (true) {
        try {
          continue;
        } finally {
          ans = null;
        }
      }
      ans = "x";
    }
  }

  String testWhile2(boolean cond) {
    String ans = "x";
    while (cond) {
      if (true) {
        return ans;
      }
      try {
        ans = null;
        continue;
      } finally {
        ans = "x";
      }
    }
    return ans;
  }

  String testWhile3(boolean cond) {
    String ans = "x";
    OUTER:
    while (true) {
      if (true) {
        // :: error: (return.type.incompatible)
        return ans;
      }

      try {
        while (cond) {
          if (true) {
            try {
              continue OUTER;
            } finally {
              ans = "x";
            }
          }
        }
      } finally {
        ans = null;
      }
      ans = "x";
    }
  }

  String testFor1() {
    String ans = "x";
    for (; ; ) {
      if (true) {
        // :: error: (return.type.incompatible)
        return ans;
      }
      if (true) {
        try {
          continue;
        } finally {
          ans = null;
        }
      }
      ans = "x";
    }
  }

  String testFor2(boolean cond) {
    String ans = "x";
    for (; cond; ) {
      if (true) {
        return ans;
      }
      try {
        ans = null;
        continue;
      } finally {
        ans = "x";
      }
    }
    return ans;
  }

  String testFor3(boolean cond) {
    String ans = "x";
    OUTER:
    for (; ; ) {
      if (true) {
        // :: error: (return.type.incompatible)
        return ans;
      }

      try {
        for (; cond; ) {
          if (true) {
            try {
              continue OUTER;
            } finally {
              ans = "x";
            }
          }
        }
      } finally {
        ans = null;
      }
      ans = "x";
    }
  }
}
