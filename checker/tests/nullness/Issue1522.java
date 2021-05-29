// Test case for Issue 1522
// https://github.com/typetools/checker-framework/issues/1522

import java.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue1522 {
  void copyInto(String p) {}

  void bar() {
    copyInto("Hi");
  }

  void copyVector(Vector<String> v, Integer[] intArray, String[] stringArray) {
    // Java types aren't compatible
    // :: error: (vector.copyinto)
    v.copyInto(intArray);
    v.copyInto(stringArray);
  }

  void copyStack(SubClassVector<String> v, Integer[] intArray, String[] stringArray) {
    // Java types aren't compatible
    // :: error: (vector.copyinto)
    v.copyInto(intArray);
    v.copyInto(stringArray);
  }

  void copyVectorErrors(Vector<@Nullable String> v, String[] stringArray) {
    // :: error: (vector.copyinto)
    v.copyInto(stringArray);
  }

  void copyStackErrors(SubClassVector<@Nullable String> v, String[] stringArray) {
    // :: error: (vector.copyinto)
    v.copyInto(stringArray);
  }

  void copyVectorNullable(Vector<@Nullable String> v, @Nullable String[] stringArray) {
    v.copyInto(stringArray);
  }

  void copyStackNullable(SubClassVector<@Nullable String> v, @Nullable String[] stringArray) {
    v.copyInto(stringArray);
  }

  static class SubClassVector<T> extends Vector<T> {
    @Override
    public synchronized void copyInto(@Nullable Object[] anArray) {
      super.copyInto(anArray);
    }
  }
}
