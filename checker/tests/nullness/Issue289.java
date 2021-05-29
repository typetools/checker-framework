// Test for Issue 289:
// https://github.com/typetools/checker-framework/issues/289

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.*;

public class Issue289 {
  void simple() {
    List<Object> lo = new ArrayList<>();
    List<String> ls = new ArrayList<>();

    List<@Nullable Object> lno = new ArrayList<>();
    List<@Nullable String> lns = new ArrayList<>();

    List<List<String>> lls = new ArrayList<>();
    lls.add(new ArrayList<>());

    // TODO: add a similar test that uses method type variables.
  }

  // TODO: work on more complex examples:

  class Upper<U1> {}

  class Middle<M1, M2> extends Upper<M2> {}

  class Lower1<L1> extends Middle<L1, @Nullable String> {}

  class Lower2<L1> extends Middle<L1, @NonNull String> {}

  void complex() {
    Upper<@Nullable String> uns = new Lower1<>();
    // :: error: (assignment)
    Upper<String> us = new Lower1<>();

    // :: error: (assignment)
    uns = new Lower2<>();
    us = new Lower2<>();
  }
}
