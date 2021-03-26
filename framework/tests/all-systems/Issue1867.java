// Test case for Issue 1867
// https://github.com/typetools/checker-framework/issues/1867

import java.util.List;

public abstract class Issue1867 {
  interface AInterface {}

  interface BInterface<X extends AInterface> {
    List<? extends X> g();
  }

  abstract List<? extends BInterface<? extends AInterface>> h();

  void f() {
    for (BInterface<? extends AInterface> x : h()) {
      for (AInterface y : x.g()) {}
    }
  }
}
