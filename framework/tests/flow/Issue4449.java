import org.checkerframework.dataflow.qual.SideEffectFree;

public class Issue4449 {

  @SideEffectFree
  public void test1(long[] x) {
    // :: error: [purity.assign.array]
    x[0] = 1;

    long y;

    // :: error: [purity.assign.array]
    ++x[0];
    // :: error: [purity.assign.array]
    --x[0];
    // :: error: [purity.assign.array]
    x[0]++;
    // :: error: [purity.assign.array]
    x[0]--;

    // :: error: [purity.assign.array]
    y = ++x[0];
    // :: error: [purity.assign.array]
    y = --x[0];
    // :: error: [purity.assign.array]
    y = x[0]++;
    // :: error: [purity.assign.array]
    y = x[0]--;

    y = +x[0];
    y = -x[0];
  }
}
