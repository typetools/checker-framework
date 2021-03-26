// The type qualifier hierarchy is: @Tainted :> @Untainted
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class FakeOverrideReturn {

  @Tainted int tf;

  @Untainted int uf;

  void m(@Tainted int t, @Untainted int u) {

    FakeOverrideRSuper sup = new FakeOverrideRSuper();
    FakeOverrideRMid mid = new FakeOverrideRMid();
    FakeOverrideRSub sub = new FakeOverrideRSub();

    tf = sup.returnsTaintedInt();
    tf = mid.returnsTaintedInt();
    tf = sub.returnsTaintedInt();
    // :: error: (assignment.type.incompatible)
    uf = sup.returnsTaintedInt();
    // :: error: (assignment.type.incompatible)
    uf = mid.returnsTaintedInt();
    // :: error: (assignment.type.incompatible)
    uf = sub.returnsTaintedInt();

    tf = sup.returnsUntaintedInt();
    tf = mid.returnsUntaintedInt();
    tf = sub.returnsUntaintedInt();
    uf = sup.returnsUntaintedInt();
    uf = mid.returnsUntaintedInt();
    uf = sub.returnsUntaintedInt();

    tf = sup.returnsTaintedIntWithFakeOverride();
    tf = mid.returnsTaintedIntWithFakeOverride();
    tf = sub.returnsTaintedIntWithFakeOverride();
    // :: error: (assignment.type.incompatible)
    uf = sup.returnsTaintedIntWithFakeOverride();
    uf = mid.returnsTaintedIntWithFakeOverride();
    uf = sub.returnsTaintedIntWithFakeOverride();

    tf = sup.returnsUntaintedIntWithFakeOverride();
    tf = mid.returnsUntaintedIntWithFakeOverride();
    tf = sub.returnsUntaintedIntWithFakeOverride();
    uf = sup.returnsUntaintedIntWithFakeOverride();
    // :: error: (assignment.type.incompatible)
    uf = mid.returnsUntaintedIntWithFakeOverride();
    // :: error: (assignment.type.incompatible)
    uf = sub.returnsUntaintedIntWithFakeOverride();
  }

  void poly() {
    FakeOverrideRSuper sup = new FakeOverrideRSuper();
    FakeOverrideRMid mid = new FakeOverrideRMid();

    @Untainted int j = mid.returnsUntaintedIntWithFakeOverride2();
    @Untainted int k = sup.returnsPolyTaintedIntWithFakeOverride();
  }
}
