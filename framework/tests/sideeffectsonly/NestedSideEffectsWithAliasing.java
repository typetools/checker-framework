import java.util.ArrayList;
import java.util.List;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class NestedSideEffectsWithAliasing {

  @SideEffectsOnly({"#1.inner.nestedList", "#1.inner"})
  void test1(OuterWrapper first) {
    List<Integer> aliasOfNestedList = first.inner.nestedList;
    List<Integer> aliasOfAlias = aliasOfNestedList;
    aliasOfAlias.add(1); // Should be OK
  }

  class OuterWrapper {
    InnerWrapper inner = new InnerWrapper();
    List<Integer> arrB = new ArrayList<>();
  }

  class InnerWrapper {
    List<Integer> nestedList = new ArrayList<>();
  }
}
