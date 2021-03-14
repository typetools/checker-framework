// Test case for issue #238: https://github.com/typetools/checker-framework/issues/238

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;

public class TestInfer {
  <T extends Object> T getValue(List<T> l) {
    return l.get(0);
  }

  void bar(Object o) {}

  void foo() {
    List<@UnknownKeyFor ? extends Object> ls = new ArrayList<>();
    bar(getValue(ls)); // this fails, but just getValue(ls) is OK
    // casting is also OK, ie bar((Object)getValue(ls))
    // The constraint should be T<:Object, which should typecheck since ls:List<? extends Object>
    // unifies with List<T> where T<:Object.
  }
}
