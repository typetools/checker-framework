// A call that appears only after the compiler desugars the source code is checked just as an
// explicitly written call is:  the `iterator()`, `hasNext()`, and `next()` calls of an enhanced
// `for` loop, and the `close()` call of a try-with-resources statement.

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class DesugaredCalls {

  static List<String> log = new ArrayList<>();

  // The iterator that an enhanced `for` loop creates did not exist before the call, so no caller
  // can observe a modification of it.  `Collection.iterator()` is `@SideEffectFree`,
  // `Iterator.hasNext()` is `@Pure`, and `Iterator.next()` is `@SideEffectsOnly("this")`, whose
  // receiver is that iterator.
  @SideEffectsOnly("this")
  void enhancedForOverCollection(List<String> lst) {
    for (String s : lst) {}
  }

  // Iterating over an array involves no call at all.
  @SideEffectsOnly("this")
  void enhancedForOverArray(String[] a) {
    for (String s : a) {}
  }

  static class Registry implements Iterable<String> {
    @Override
    @SideEffectsOnly("DesugaredCalls.log")
    public Iterator<String> iterator() {
      log.add("iterated");
      return Collections.<String>emptyList().iterator();
    }
  }

  @SideEffectsOnly("this")
  void enhancedForCallsIterator(Registry r) {
    // The implicit call to `r.iterator()` modifies `DesugaredCalls.log`, which this method's
    // annotation does not list.
    // :: error: (purity.incorrect.sideeffectsonly)
    for (String s : r) {}
  }

  @SideEffectsOnly("DesugaredCalls.log")
  void enhancedForCallsIteratorPermitted(Registry r) {
    // `r.iterator()` modifies only `DesugaredCalls.log`, which this method's annotation lists.
    for (String s : r) {}
  }

  static class UnannotatedIterable implements Iterable<String> {
    @Override
    public Iterator<String> iterator() {
      return Collections.<String>emptyList().iterator();
    }
  }

  @SideEffectsOnly("this")
  void enhancedForCallsUnannotatedIterator(UnannotatedIterable u) {
    // `iterator()` has no side-effect annotation, so it might modify arbitrary state.
    // :: error: (purity.unknown.sideeffectsonly)
    for (String s : u) {}
  }

  // `StringIterator.next()` returns `String` where `Iterator<String>.next()` erases to `Object`,
  // and `CovariantRegistry.iterator()` returns `StringIterator` where `Iterable.iterator()` erases
  // to `Iterator`, so javac adds a bridge method to each class.  A bridge carries none of the
  // annotations of the method it forwards to, so the loop below would report
  // `purity.unknown.sideeffectsonly` if a member lookup returned one.  Which of the two members a
  // lookup encounters first is unspecified, so this test exercises the lookup rather than pinning
  // down its order.
  static class StringIterator implements Iterator<String> {
    @SideEffectFree
    StringIterator() {}

    @Override
    @Pure
    public boolean hasNext() {
      return false;
    }

    @Override
    @SideEffectsOnly("this")
    public String next() {
      throw new java.util.NoSuchElementException();
    }
  }

  static class CovariantRegistry implements Iterable<String> {
    @Override
    @SideEffectFree
    public StringIterator iterator() {
      return new StringIterator();
    }
  }

  @SideEffectsOnly("this")
  void enhancedForOverCovariantIterator(CovariantRegistry r) {
    for (String s : r) {}
  }

  static class Resource implements AutoCloseable {
    @SideEffectsOnly("this")
    Resource() {}

    @Override
    @SideEffectsOnly("DesugaredCalls.log")
    public void close() {
      log.add("closed");
    }
  }

  @SideEffectsOnly("this")
  void tryWithResourcesCallsClose() {
    // The implicit call to `r.close()` modifies `DesugaredCalls.log`.
    // :: error: (purity.incorrect.sideeffectsonly)
    try (Resource r = new Resource()) {}
  }

  @SideEffectsOnly("DesugaredCalls.log")
  void tryWithResourcesCallsClosePermitted() {
    try (Resource r = new Resource()) {}
  }

  static class SelfClosingResource implements AutoCloseable {
    boolean closed;

    @SideEffectsOnly("this")
    SelfClosingResource() {}

    @Override
    @SideEffectsOnly("this")
    public void close() {
      closed = true;
    }
  }

  // The resource is an object that this method created, so closing it is not visible to the
  // caller.
  @SideEffectsOnly("this")
  void tryWithResourcesOnFreshResource() {
    try (SelfClosingResource r = new SelfClosingResource()) {}
  }

  @SideEffectsOnly("this")
  void tryWithResourcesOnUnannotatedClose(Reader reader) throws IOException {
    // `Reader.close()` has no side-effect annotation, so it might modify arbitrary state.
    // :: error: (purity.unknown.sideeffectsonly)
    try (Reader r = reader) {}
  }
}
