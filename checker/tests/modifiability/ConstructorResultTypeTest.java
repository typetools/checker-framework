// Tests the `inconsistent.constructor.result.type` and `bottom.annotation.on.receiver` checks,
// which are performed by ModifiabilityBaseVisitor.processClassTree.
//
// A class below whose constructors are @Growable also gets an `inherited.implementation.uoe`
// error, because it extends AbstractList without overriding `add(int, E)`, whose implementation
// throws UnsupportedOperationException.  See InheritedImplementationTest.java.

import java.util.AbstractList;
import java.util.List;
import org.checkerframework.checker.modifiability.qual.BottomGrowable;
import org.checkerframework.checker.modifiability.qual.Growable;
import org.checkerframework.checker.modifiability.qual.Ungrowable;

public class ConstructorResultTypeTest {

  // All the constructors declare the same result qualifier, so there is no
  // `inconsistent.constructor.result.type` error.
  // :: error: [inherited.implementation.uoe]
  static class ConsistentConstructors extends AbstractList<String> {
    @Growable ConsistentConstructors() {}

    @Growable ConsistentConstructors(int capacity) {}

    @Override
    public String get(int index) {
      return "value";
    }

    @Override
    public int size() {
      return 0;
    }
  }

  // The second constructor disagrees with the first.
  static class InconsistentConstructors extends AbstractList<String> {
    @Growable InconsistentConstructors() {}

    // :: error: [inconsistent.constructor.result.type]
    @Ungrowable InconsistentConstructors(int capacity) {}

    @Override
    public String get(int index) {
      return "value";
    }

    @Override
    public int size() {
      return 0;
    }
  }

  // The bottom qualifier is meaningless on a receiver, so writing it is an error.
  // :: error: [inherited.implementation.uoe]
  static class BottomReceiver extends AbstractList<String> {
    @Growable BottomReceiver() {}

    @Override
    public String get(int index) {
      return "value";
    }

    @Override
    public int size() {
      return 0;
    }

    // :: error: [bottom.annotation.on.receiver]
    public void bottomReceiver(@BottomGrowable BottomReceiver this) {}
  }

  // The interface declares no constructor, so the checks about constructors do nothing, but the
  // bottom qualifier on a receiver is still an error.
  interface NoConstructor extends List<String> {
    // :: error: [bottom.annotation.on.receiver]
    default void bottomReceiver(@BottomGrowable NoConstructor this) {}
  }

  // The constructors disagree, but the bottom qualifier on a receiver is still an error.
  static class InconsistentConstructorsAndBottomReceiver extends AbstractList<String> {
    @Growable InconsistentConstructorsAndBottomReceiver() {}

    // :: error: [inconsistent.constructor.result.type]
    @Ungrowable InconsistentConstructorsAndBottomReceiver(int capacity) {}

    @Override
    public String get(int index) {
      return "value";
    }

    @Override
    public int size() {
      return 0;
    }

    // :: error: [bottom.annotation.on.receiver]
    public void bottomReceiver(@BottomGrowable InconsistentConstructorsAndBottomReceiver this) {}
  }

  // The method has no body, but the bottom qualifier on its receiver is still an error.
  // :: error: [inherited.implementation.uoe]
  abstract static class AbstractBottomReceiver extends AbstractList<String> {
    @Growable AbstractBottomReceiver() {}

    // :: error: [bottom.annotation.on.receiver]
    public abstract void bottomReceiver(@BottomGrowable AbstractBottomReceiver this);
  }
}
