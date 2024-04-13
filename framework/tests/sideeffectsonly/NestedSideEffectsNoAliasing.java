import java.util.ArrayList;
import java.util.List;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class NestedSideEffectsNoAliasing {

  @SideEffectsOnly("#1.inner.nestedList")
  void test1(OuterWrapper first) {
    first.inner.nestedList.add(1); // OK
    // :: error: (purity.incorrect.sideeffectsonly)
    first.arrB.add(2);
  }

  class OuterWrapper {
    InnerWrapper inner = new InnerWrapper();
    List<Integer> arrB = new ArrayList<>();
  }

  class InnerWrapper {
    List<Integer> nestedList = new ArrayList<>();
  }
}
