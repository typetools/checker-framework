import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue3929 {

  public void endElement(MyClass3929 arg) {
    for (Object o : arg.getKeys()) {
      o.toString();
    }
  }

  public void endElement(NullableMyClass3929 arg) {
    for (Object o : arg.getKeys()) {
      // :: error: (dereference.of.nullable)
      o.toString();
    }
  }
}

class MyClass3929<K extends Comparable<K>> {
  public List<K> getKeys() {
    return new ArrayList<>();
  }
}
// TODO: This is a false positive.
// See https://github.com/typetools/checker-framework/issues/2174
// :: error: (type.argument.type.incompatible)
class NullableMyClass3929<K extends @Nullable Comparable<K>> {
  public List<K> getKeys() {
    return new ArrayList<>();
  }
}
