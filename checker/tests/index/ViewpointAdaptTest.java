// @skip-test

import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;

public class ViewpointAdaptTest {

  void ListGet(
      @LTLengthOf("list") int index, @LTEqLengthOf("list") int notIndex, List<Integer> list) {
    // :: error: (argument)
    list.get(index);

    // :: error: (argument)
    list.get(notIndex);
  }
}
