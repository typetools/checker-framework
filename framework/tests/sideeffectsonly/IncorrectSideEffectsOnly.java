package sideeffectsonly;

import java.util.Collection;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class IncorrectSideEffectsOnly {
  @SideEffectsOnly({"#2"})
  void test(Collection<Integer> cl, Collection<Integer> cl2) {
    cl.add(9);
  }
}
