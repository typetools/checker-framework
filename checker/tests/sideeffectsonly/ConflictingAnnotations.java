import java.util.Collection;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class ConflictingAnnotations {

  @SideEffectsOnly("#1")
  @SideEffectFree
  // :: error: (purity.annotation.conflict)
  void test1(Collection<Integer> first) {
    // :: error: (purity.call)
    first.add(1);
  }

  @SideEffectsOnly("#2")
  @Pure
  // :: error: (purity.annotation.conflict)
  int test2(Collection<Integer> first, Collection<Integer> second) {
    return 1;
  }

  // Writing an alias for @Pure is the same as writing @Pure.
  @SideEffectsOnly("#1")
  @org.jmlspecs.annotation.Pure
  // :: error: (purity.annotation.conflict)
  int test3(Collection<Integer> first) {
    return 1;
  }

  static class PureSuper {
    @Pure
    int m() {
      return 1;
    }
  }

  static class WritesSideEffectFree extends PureSuper {
    // The method inherits @Pure from `PureSuper.m` and writes @SideEffectFree.  The two written
    // annotations conflict, even though the @Pure annotation that is inherited does not.
    @Override
    @SideEffectsOnly("this")
    @SideEffectFree
    // :: error: (purity.annotation.conflict)
    int m() {
      return 1;
    }
  }
}
