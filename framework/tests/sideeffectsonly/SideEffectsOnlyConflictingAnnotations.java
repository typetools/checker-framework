import java.util.Collection;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class SideEffectsOnlyConflictingAnnotations {

  @SideEffectsOnly("#1")
  @SideEffectFree
  // :: error: (purity.incorrect.annotation.conflict)
  void test1(Collection<Integer> first) {
    // :: error: (purity.not.sideeffectfree.call)
    first.add(1);
  }

  @SideEffectsOnly("#2")
  @Pure
  // :: error: (purity.incorrect.annotation.conflict)
  int test2(Collection<Integer> first, Collection<Integer> second) {
    return 1;
  }
}
