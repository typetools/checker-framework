// A member accessed through a receiver whose type is a type variable whose bound is raw is erased,
// just as if the receiver had the raw bound itself.  javac warns "unchecked call to
// put(Desc<@Nullable String>) as a member of the raw type Backend" for all three calls below, and
// "unchecked conversion ... found: Desc" for all three assignments.
//
// AnnotatedTypes.asMemberOf looks through a TYPEVAR (and a WILDCARD) to its upper bound before
// testing whether the access is on a raw type, so `typeVariableReceiver` and
// `capturedWildcardReceiver` are erased.  The INTERSECTION case substitutes into the member's type
// for each bound without testing any of them for rawness, so `intersectionBoundReceiver` is not.

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings({"rawtypes", "unchecked"})
public class RawTypeVariableReceiver {

  static class Desc<T> {}

  interface Backend<K> {
    void put(Desc<@Nullable String> d);

    Desc<@Nullable String> get();
  }

  // The bound is parameterized, so nothing is erased.
  <B extends Backend<Object>> void parameterizedBound(B b, Desc<@Nullable String> d) {
    b.put(d);
    Desc<@Nullable String> x = b.get();
  }

  <B extends Backend> void typeVariableReceiver(B raw, Desc<String> d) {
    raw.put(d);
    Desc<String> x = raw.get();
  }

  // The type of `l.get(0)` is the capture of `? extends Backend`, a type variable whose upper
  // bound is the raw type `Backend`.
  void capturedWildcardReceiver(List<? extends Backend> l, Desc<String> d) {
    l.get(0).put(d);
    Desc<String> x = l.get(0).get();
  }

  <B extends Backend & Cloneable> void intersectionBoundReceiver(B raw, Desc<String> d) {
    // TODO: This error is a false positive: javac erases the signature of `put` here, just as it
    // does in `typeVariableReceiver` above.  Remove this expected diagnostic when the INTERSECTION
    // case of AnnotatedTypes.asMemberOf tests its bounds for rawness.
    // :: error: (argument)
    raw.put(d);
    Desc<String> x = raw.get();
  }
}
