// Test case for Issue #425:
// https://github.com/typetools/checker-framework/issues/425

// @skip-test until the issue is fixed

import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue425 {
  private @Nullable Set<Integer> field = null;

  class EvilSet<T> extends HashSet<T> {
    public boolean add(T e) {
      field = null;
      return super.add(e);
    }
  }

  public void fail() {
    if (field == null) {
      field = new EvilSet<>();
    }

    field.add(1);
    // This line throws an exception at run time.
    field.add(2);
  }

  public static void main(String[] args) throws Exception {
    Issue425 m = new Issue425();
    m.fail();
  }
}
