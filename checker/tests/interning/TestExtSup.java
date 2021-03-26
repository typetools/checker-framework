// Test case for issue #237: https://github.com/typetools/checker-framework/issues/237

import java.util.List;

interface A<T> {
  public abstract int transform(List<? super T> function);
}

class B implements A<Object> {
  @Override
  public int transform(List<Object> function) {
    return 0;
  }
}

public class TestExtSup {}
