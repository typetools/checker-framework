// Tests the `method.implementation.is.uoe` and `method.implementation.not.uoe` checks, which
// compare a method body against the modifiability annotation on the class's constructors.
//
// A class below whose constructors are @Growable also gets an `inherited.implementation.uoe`
// error, because it extends AbstractList without overriding `add(int, E)`, whose implementation
// throws UnsupportedOperationException.  See InheritedImplementationTest.java.

import java.util.AbstractList;
import org.checkerframework.checker.modifiability.qual.Growable;
import org.checkerframework.checker.modifiability.qual.IteratorPolyMod;
import org.checkerframework.checker.modifiability.qual.SeqGrowable;
import org.checkerframework.checker.modifiability.qual.SeqUngrowable;
import org.checkerframework.checker.modifiability.qual.Ungrowable;

public class UoeImplementationTest {

  // All constructors are @Ungrowable, so a method with a @Growable receiver must throw
  // UnsupportedOperationException.
  static class UngrowableList extends AbstractList<String> {
    @Ungrowable UngrowableList() {}

    @Override
    public String get(int index) {
      return "value";
    }

    @Override
    public int size() {
      return 0;
    }

    public void growThrows(@Growable UngrowableList this) {
      throw new UnsupportedOperationException();
    }

    // :: error: [method.implementation.not.uoe]
    public void growDoesNotThrow(@Growable UngrowableList this) {}
  }

  // All constructors are @Growable, so a method with a @Growable receiver must not throw
  // UnsupportedOperationException.
  // :: error: [inherited.implementation.uoe]
  static class GrowableList extends AbstractList<String> {
    @Growable GrowableList() {}

    @Override
    public String get(int index) {
      return "value";
    }

    @Override
    public int size() {
      return 0;
    }

    public void growDoesNotThrow(@Growable GrowableList this) {}

    // :: error: [method.implementation.is.uoe]
    public void growThrows(@Growable GrowableList this) {
      throw new UnsupportedOperationException();
    }
  }

  // `add()` states no requirement on its own receiver, but it inherits one from `List.add()`,
  // which it overrides.  All the constructors are @Growable, so `add()` must not throw
  // UnsupportedOperationException.
  // :: error: [inherited.implementation.uoe]
  static @Growable class ClassLevelGrowableList extends AbstractList<String> {
    @Growable ClassLevelGrowableList() {}

    @Override
    public String get(int index) {
      return "value";
    }

    @Override
    public int size() {
      return 0;
    }

    @Override
    // :: error: [method.implementation.is.uoe]
    public boolean add(String element) {
      throw new UnsupportedOperationException();
    }
  }

  // A method with no body has no implementation to check.
  abstract static class AbstractUngrowableList extends AbstractList<String> {
    @Ungrowable AbstractUngrowableList() {}

    public abstract void growAbstract(@Growable AbstractUngrowableList this);
  }

  // The SeqGrow hierarchy behaves like the Grow hierarchy, even though its negative qualifier is
  // named `@SeqUngrowable` rather than starting with "Un".
  static class SeqUngrowableList extends AbstractList<String> {
    @SeqUngrowable SeqUngrowableList() {}

    @Override
    public String get(int index) {
      return "value";
    }

    @Override
    public int size() {
      return 0;
    }

    public void seqGrowThrows(@SeqGrowable SeqUngrowableList this) {
      throw new UnsupportedOperationException();
    }

    // :: error: [method.implementation.not.uoe]
    public void seqGrowDoesNotThrow(@SeqGrowable SeqUngrowableList this) {}
  }

  static class SeqGrowableList extends AbstractList<String> {
    @SeqGrowable SeqGrowableList() {}

    @Override
    public String get(int index) {
      return "value";
    }

    @Override
    public int size() {
      return 0;
    }

    public void seqGrowDoesNotThrow(@SeqGrowable SeqGrowableList this) {}

    // :: error: [method.implementation.is.uoe]
    public void seqGrowThrows(@SeqGrowable SeqGrowableList this) {
      throw new UnsupportedOperationException();
    }
  }

  // The Iterator hierarchy has no negative qualifier: @IteratorPolyMod says what a collection's
  // iterator preserves, not whether a method throws UnsupportedOperationException.
  static class IteratorPolyModList extends AbstractList<String> {
    @IteratorPolyMod IteratorPolyModList() {}

    @Override
    public String get(int index) {
      return "value";
    }

    @Override
    public int size() {
      return 0;
    }

    public void iteratorPolyModThrows(@IteratorPolyMod IteratorPolyModList this) {
      throw new UnsupportedOperationException();
    }
  }
}
