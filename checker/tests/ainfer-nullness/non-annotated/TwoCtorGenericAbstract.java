// test case for

import java.util.Set;

public abstract class TwoCtorGenericAbstract<T extends Object> implements Set<T> {
  protected T value;

  protected AbstractMostlySingletonSetUnannotated() {
    // :: warning: assignment
    this.value = null;
  }

  protected AbstractMostlySingletonSetUnannotated(T v) {
    this.value = v;
  }
}
