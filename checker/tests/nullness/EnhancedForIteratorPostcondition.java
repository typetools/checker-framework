// The desugared call to iterator() of an enhanced for loop can throw an exception before it
// establishes its postcondition.

import java.util.Collections;
import java.util.Iterator;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

class EnhancedForIteratorPostcondition {

  static class FieldSettingIterable implements Iterable<String> {
    @MonotonicNonNull Object f;

    @Override
    @EnsuresNonNull("f")
    public Iterator<String> iterator() {
      f = new Object();
      return Collections.emptyIterator();
    }
  }

  void afterLoop(FieldSettingIterable it) {
    for (String s : it) {}
    it.f.toString();
  }

  void inCatch(FieldSettingIterable it) {
    try {
      for (String s : it) {}
    } catch (RuntimeException e) {
      // :: error: (dereference.of.nullable)
      it.f.toString();
    }
  }
}
