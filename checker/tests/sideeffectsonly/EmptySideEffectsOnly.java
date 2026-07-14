import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class EmptySideEffectsOnly {
  @SideEffectsOnly({})
  // :: error: [purity.empty.sideeffectsonly] :: warning: [purity.more.sideeffectfree]
  void method1(@Untainted Object x) {}
}
