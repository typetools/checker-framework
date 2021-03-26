import org.checkerframework.dataflow.qual.SideEffectFree;

public class Issue4449 {

  @SideEffectFree
  public void test1(long[] x) {
    // :: error: (purity.not.sideeffectfree.assign.array)
    x[0] = 1;

    long y;

    // :: error: (purity.not.sideeffectfree.assign.array)
    ++x[0];
    // :: error: (purity.not.sideeffectfree.assign.array)
    --x[0];
    // :: error: (purity.not.sideeffectfree.assign.array)
    x[0]++;
    // :: error: (purity.not.sideeffectfree.assign.array)
    x[0]--;

    // :: error: (purity.not.sideeffectfree.assign.array)
    y = ++x[0];
    // :: error: (purity.not.sideeffectfree.assign.array)
    y = --x[0];
    // :: error: (purity.not.sideeffectfree.assign.array)
    y = x[0]++;
    // :: error: (purity.not.sideeffectfree.assign.array)
    y = x[0]--;

    y = +x[0];
    y = -x[0];
  }
}
