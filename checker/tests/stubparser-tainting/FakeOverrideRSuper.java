// The type qualifier hierarchy is: @Tainted :> @Untainted
import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

@SuppressWarnings("tainting")
public class FakeOverrideRSuper {

  public @Tainted int returnsTaintedInt() {
    return 0;
  }

  public @Untainted int returnsUntaintedInt() {
    return 0;
  }

  public @Tainted int returnsTaintedIntWithFakeOverride() {
    return 0;
  }

  public @Untainted int returnsUntaintedIntWithFakeOverride() {
    return 0;
  }

  public @Untainted int returnsUntaintedIntWithFakeOverride2() {
    return 0;
  }

  public @PolyTainted int returnsPolyTaintedIntWithFakeOverride() {
    return 0;
  }
}
