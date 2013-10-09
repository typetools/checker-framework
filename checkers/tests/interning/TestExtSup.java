// Test case for issue #237: https://code.google.com/p/checker-framework/issues/detail?id=237

import java.util.*;

interface A<T> {
  public abstract int transform(List<? super T> function);
} 

class B implements A<Object> {
  @Override
  public int transform(List<Object> function) {
    return 0;
  }
}

public class TestExtSup {
}
