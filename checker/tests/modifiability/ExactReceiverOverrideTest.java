import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import org.checkerframework.checker.modifiability.qual.Growable;
import org.checkerframework.checker.modifiability.qual.Modifiable;
import org.checkerframework.checker.modifiability.qual.Replaceable;
import org.checkerframework.checker.modifiability.qual.Shrinkable;
import org.checkerframework.checker.modifiability.qual.Unmodifiable;

public class ExactReceiverOverrideTest {

  static class GrowParent extends ArrayList<String> {
    void update(@Growable GrowParent this) {}
  }

  static class GrowChildImplicit extends GrowParent {
    @Override
    // :: error: [override.receiver]
    void update() {}
  }

  static class GrowChildExact extends GrowParent {
    @Override
    void update(@Growable GrowChildExact this) {}
  }

  static class ShrinkParent extends ArrayList<String> {
    void update(@Shrinkable ShrinkParent this) {}
  }

  static class ShrinkChildImplicit extends ShrinkParent {
    @Override
    // :: error: [override.receiver]
    void update() {}
  }

  static class ShrinkChildExact extends ShrinkParent {
    @Override
    void update(@Shrinkable ShrinkChildExact this) {}
  }

  static class ReplaceParent extends ArrayList<String> {
    void update(@Replaceable ReplaceParent this) {}
  }

  static class ReplaceChildImplicit extends ReplaceParent {
    @Override
    // :: error: [override.receiver]
    void update() {}
  }

  static class ReplaceChildExact extends ReplaceParent {
    @Override
    void update(@Replaceable ReplaceChildExact this) {}
  }

  static class ModifiableParent extends ArrayList<String> {
    void update(@Modifiable ModifiableParent this) {}
  }

  static class ModifiableChildImplicit extends ModifiableParent {
    @Override
    // :: error: [override.receiver] :: error: [override.receiver] :: error: [override.receiver]
    void update() {}
  }

  static class ModifiableChildExact extends ModifiableParent {
    @Override
    void update(@Modifiable ModifiableChildExact this) {}
  }

  static class GrowShrinkParent extends ArrayList<String> {
    void update(@Growable @Shrinkable GrowShrinkParent this) {}
  }

  static class GrowOnlyChild extends GrowShrinkParent {
    @Override
    // :: error: [override.receiver]
    void update(@Growable GrowOnlyChild this) {}
  }

  static class ShrinkOnlyChild extends GrowShrinkParent {
    @Override
    // :: error: [override.receiver]
    void update(@Shrinkable ShrinkOnlyChild this) {}
  }

  static class UnmodifiableParent extends ArrayList<String> {
    void update(@Unmodifiable UnmodifiableParent this) {}
  }

  static class UnmodifiableChildImplicit extends UnmodifiableParent {
    @Override
    void update() {}
  }

  static class UnmodifiableArrayList<E> extends ArrayList<E> {

    @Unmodifiable UnmodifiableArrayList() {}

    @Override
    public boolean add(@Growable UnmodifiableArrayList<E> this, E element) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void add(@Growable UnmodifiableArrayList<E> this, int index, E element) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(
        @Growable UnmodifiableArrayList<E> this, Collection<? extends E> collection) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(
        @Growable UnmodifiableArrayList<E> this, int index, Collection<? extends E> collection) {
      throw new UnsupportedOperationException();
    }

    @Override
    public E remove(@Shrinkable UnmodifiableArrayList<E> this, int index) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(@Shrinkable UnmodifiableArrayList<E> this, Object element) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@Shrinkable UnmodifiableArrayList<E> this, Collection<?> collection) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(@Shrinkable UnmodifiableArrayList<E> this, Collection<?> collection) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeIf(
        @Shrinkable UnmodifiableArrayList<E> this, Predicate<? super E> filter) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear(@Shrinkable UnmodifiableArrayList<E> this) {
      throw new UnsupportedOperationException();
    }

    @Override
    // :: error: [override.receiver]
    public E set(UnmodifiableArrayList<E> this, int index, E element) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void replaceAll(@Replaceable UnmodifiableArrayList<E> this, UnaryOperator<E> operator) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void sort(@Replaceable UnmodifiableArrayList<E> this, Comparator<? super E> comparator) {
      throw new UnsupportedOperationException();
    }
  }
}
