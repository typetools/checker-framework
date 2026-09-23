// Tests the `inherited.implementation.uoe` check, which is about a method whose implementation the
// class inherits without overriding, and the `throwsuoe.implementation.not.uoe` check, which
// verifies the @ThrowsUnsupportedOperation annotation itself.

import java.util.AbstractList;
import org.checkerframework.checker.modifiability.qual.Growable;
import org.checkerframework.checker.modifiability.qual.ThrowsUnsupportedOperation;
import org.checkerframework.checker.modifiability.qual.Ungrowable;

public class InheritedImplementationTest {

  // `AbstractList.add(int, E)` is annotated @ThrowsUnsupportedOperation, and this class overrides
  // it, so the class does support growing.
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

    @Override
    public void add(@Growable GrowableList this, int index, String element) {}
  }

  // All the constructors are @Ungrowable, so `growThrows()` was checked to throw
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
  }

  // The class inherits `growThrows()`, which throws, and `AbstractList.add(int, E)`, which is
  // annotated @ThrowsUnsupportedOperation.  It cannot claim to be @Growable.
  // :: error: [inherited.implementation.uoe] :: error: [inherited.implementation.uoe]
  static class GrowableSubclass extends UngrowableList {
    @Growable GrowableSubclass() {}
  }

  // Overriding both of them is enough.
  static class GrowableSubclassThatOverrides extends UngrowableList {
    @Growable GrowableSubclassThatOverrides() {}

    @Override
    public void growThrows(@Growable GrowableSubclassThatOverrides this) {}

    @Override
    public void add(@Growable GrowableSubclassThatOverrides this, int index, String element) {}
  }

  // An abstract class is not checked for inherited implementations; its concrete subclasses are.
  abstract static class AbstractGrowableBase<E> extends AbstractList<E> {
    @Growable AbstractGrowableBase() {}
  }

  static class ConcreteSubclassThatOverrides extends AbstractGrowableBase<String> {
    @Growable ConcreteSubclassThatOverrides() {}

    @Override
    public String get(int index) {
      return "value";
    }

    @Override
    public int size() {
      return 0;
    }

    @Override
    public void add(@Growable ConcreteSubclassThatOverrides this, int index, String element) {}
  }

  // :: error: [inherited.implementation.uoe]
  static class ConcreteSubclassThatDoesNotOverride extends AbstractGrowableBase<String> {
    @Growable ConcreteSubclassThatDoesNotOverride() {}

    @Override
    public String get(int index) {
      return "value";
    }

    @Override
    public int size() {
      return 0;
    }
  }

  // The constructors disagree, so the method bodies of this class were not checked, and nothing is
  // known about whether `doesNotThrow()` throws.  (It does not.)
  static class InconsistentConstructors extends AbstractList<String> {
    @Ungrowable InconsistentConstructors() {}

    // :: error: [inconsistent.constructor.result.type]
    @Growable InconsistentConstructors(int capacity) {}

    @Override
    public String get(int index) {
      return "value";
    }

    @Override
    public int size() {
      return 0;
    }

    public void doesNotThrow(@Growable InconsistentConstructors this) {}

    @Override
    public void add(@Growable InconsistentConstructors this, int index, String element) {}
  }

  // The subclass inherits `doesNotThrow()`, which is not known to throw.
  static class GrowableSubclassOfInconsistent extends InconsistentConstructors {
    @Growable GrowableSubclassOfInconsistent() {}
  }

  /** A subclass of UnsupportedOperationException is an UnsupportedOperationException. */
  static class MyUnsupportedOperationException extends UnsupportedOperationException {}

  // A method whose body throws a subclass of UnsupportedOperationException does not support the
  // operation, just as one that throws UnsupportedOperationException itself does not.
  static class ThrowsSubclass extends AbstractList<String> {
    @Ungrowable ThrowsSubclass() {}

    @Override
    public String get(int index) {
      return "value";
    }

    @Override
    public int size() {
      return 0;
    }

    public void growThrows(@Growable ThrowsSubclass this) {
      throw new MyUnsupportedOperationException();
    }
  }

  // The checker verifies @ThrowsUnsupportedOperation on every method that it compiles.
  abstract static class AnnotationVerification extends AbstractList<String> {
    @Ungrowable AnnotationVerification() {}

    @ThrowsUnsupportedOperation
    public void throwsUoe(@Growable AnnotationVerification this) {
      throw new UnsupportedOperationException();
    }

    @ThrowsUnsupportedOperation
    public void throwsSubclassOfUoe(@Growable AnnotationVerification this) {
      throw new MyUnsupportedOperationException();
    }

    @ThrowsUnsupportedOperation
    // :: error: [throwsuoe.implementation.not.uoe] :: error: [method.implementation.not.uoe]
    public void doesNotThrow(@Growable AnnotationVerification this) {}

    // An abstract method has no implementation to make the promise about.
    @ThrowsUnsupportedOperation
    // :: error: [throwsuoe.implementation.not.uoe]
    public abstract void noBody(@Growable AnnotationVerification this);
  }
}
