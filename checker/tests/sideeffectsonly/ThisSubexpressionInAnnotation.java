// An annotation expression that is `this` denotes an object that the caller cannot refer to, so
// modifying it is not a side effect that the caller can observe.  A larger expression that merely
// contains `this` gets no such exemption:  its value may be an object that existed before the call.

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class ThisSubexpressionInAnnotation {

  static class Aliaser {
    List<String> f;

    // The constructor stores its argument in `this.f`, so modifying `this.f` modifies an object
    // that existed before the call.
    @SideEffectsOnly("this.f")
    Aliaser(List<String> p) {
      this.f = p;
    }
  }

  @SideEffectsOnly("#1")
  void callsAliaser(List<String> l) {
    // `this.f` cannot be written at this call site, and its value may be an object that existed
    // before the call, so the checker cannot tell what the constructor modifies.
    // :: error: (purity.unknown.sideeffectsonly)
    new Aliaser(l);
  }

  static class OnlyThis {
    // `this` alone is the object under construction, which no caller can refer to.
    @SideEffectsOnly("this")
    OnlyThis() {}
  }

  @SideEffectsOnly("#1")
  void callsOnlyThis(List<String> l) {
    new OnlyThis();
  }

  static class BackedIterator implements Iterator<String> {
    List<String> backing = new ArrayList<>();

    @Override
    @Pure
    public boolean hasNext() {
      return false;
    }

    // `this` is the iterator, but `this.backing` may be a list that existed before the iterator
    // was created.
    @Override
    @SideEffectsOnly("this.backing")
    public String next() {
      return null;
    }
  }

  static class Backed implements Iterable<String> {
    BackedIterator iter = new BackedIterator();

    @Override
    @SideEffectFree
    public BackedIterator iterator() {
      return iter;
    }
  }

  @SideEffectsOnly("this")
  void loopOverBacked(Backed b) {
    // The loop creates the iterator, so `this.backing` cannot be written here; and its value may
    // be a list that existed before the loop.
    // :: error: (purity.unknown.sideeffectsonly)
    for (String s : b) {}
  }
}
