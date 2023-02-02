// test case for https://github.com/typetools/checker-framework/issues/5524

import java.util.Set;

public abstract class TwoCtorGenericAbstract<T extends Object> implements Set<T> {
  protected T value;

  protected TwoCtorGenericAbstract() {
    // :: warning: assignment
    this.value = null;
  }

  protected TwoCtorGenericAbstract(T v) {
    this.value = v;
  }
}
