package sideeffectsonly;

import java.util.Collection;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class CheckSideEffectsOnly {

  @SideEffectsOnly({"#2"})
  void test(Collection<Integer> cl, Collection<Integer> cl2) {
    // :: error: purity.incorrect.sideeffectsonly
    cl.add(9);
    cl2.add(10);
  }

  @SideEffectsOnly({"#2"})
  static void test1(Collection<Integer> cl, Collection<Integer> cl2) {
    cl2.add(10);
  }
}
