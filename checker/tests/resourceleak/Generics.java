import java.util.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class Generics {

  static class C1<T extends List<String>> {

    private final Map<String, T> m = new HashMap<>();

    public void method(T var) {
      m.put("foo", var);
    }
  }
}
