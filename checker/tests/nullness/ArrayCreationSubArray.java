// Test case for issue #599:
// https://github.com/typetools/checker-framework/issues/599

// @skip-test Commented out until the bug is fixed

import org.checkerframework.checker.nullness.qual.*;

public class ArrayCreationSubArray {

  void m() {

    Object o1a = new Integer[] {1, 2, null, 3, 4};
    @Nullable Integer[] o1b = new Integer[] {1, 2, null, 3, 4};

    Object o2a = new Object[] {null};
    Object o2b = new @Nullable Object[] {null};

    Object o3a = new Object[][] {new Object[] {null}};
    Object o3b = new Object[][] {new @Nullable Object[] {null}};
    Object o3c = new @Nullable Object[][] {new @Nullable Object[] {null}};
    Object o3d = new Object[][] {{null}};
  }
}
