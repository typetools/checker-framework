// @skip-test until we bring list support back
import java.util.List;
import java.util.ListIterator;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;

public class ListLowerBound {

  private void m(List<Object> l) {
    // :: error: (argument.type.incompatible)
    l.get(-1);
    // :: error: (argument.type.incompatible)
    ListIterator<Object> li = l.listIterator(-1);

    @NonNegative int ni = li.nextIndex();
    @GTENegativeOne int pi = li.previousIndex();
  }
}
