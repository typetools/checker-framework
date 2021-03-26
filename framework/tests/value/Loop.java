import java.util.Iterator;
import java.util.Set;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.BottomVal;
import org.checkerframework.common.value.qual.IntVal;

public class Loop {
  void test1(final double @ArrayLen(20) [] f2) {
    int x;
    for (int g = 0; g < f2.length; g++) {
      x = g;
    }
    // :: error: (assignment.type.incompatible)
    double @BottomVal [] test = f2;
    // :: error: (assignment.type.incompatible)
    @BottomVal int q = f2.length;
  }

  void test2(final @IntVal(20) int param) {
    int x;
    for (int g = 0; g < param; g++) {
      x = g;
    }
    // :: error: (assignment.type.incompatible)
    @BottomVal int q = param;
  }

  private void test3(Set<String> set) {
    String[] array = new String[set.size()];
    int i = 0;
    for (Iterator<String> iter = set.iterator(); iter.hasNext(); ) {
      String key = iter.next();
      array[i++] = key;
    }
  }
}
