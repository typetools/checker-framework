// Test case for issue #455: https://github.com/typetools/checker-framework/issues/455

import java.util.Objects;

public final class ArraysMDE {

  public static int indexOf(Object[] a, Object[] sub) {
    int a_index_max = a.length - sub.length + 1;
    for (int i = 0; i <= a_index_max; i++) {
      if (isSubarray(a, sub, i)) {
        return i;
      }
    }
    return -1;
  }

  public static boolean isSubarray(Object[] a, Object[] sub, int a_offset) {
    int a_len = a.length - a_offset;
    int sub_len = sub.length;
    if (a_len < sub_len) {
      return false;
    }
    for (int i = 0; i < sub_len; i++) {
      if (!Objects.equals(sub[i], a[a_offset + i])) {
        return false;
      }
    }
    return true;
  }
}
