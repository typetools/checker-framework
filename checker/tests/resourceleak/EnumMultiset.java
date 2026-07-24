import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.ObjIntConsumer;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.signedness.qual.UnknownSignedness;
import org.checkerframework.framework.qual.AnnotatedFor;

/**
 * Multiset implementation specialized for enum elements, supporting all single-element operations
 * in O(1).
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/NewCollectionTypesExplained#multiset">{@code Multiset}</a>.
 *
 * @author Jared Levy
 * @since 2.0
 */
@AnnotatedFor({"nullness"})
public final class EnumMultiset<E extends Enum<E>> implements Serializable {
  /** Creates an empty {@code EnumMultiset}. */
  public static <E extends Enum<E>> EnumMultiset<E> create(Class<E> type) {
    return new EnumMultiset<E>(type);
  }

  /**
   * Creates a new {@code EnumMultiset} containing the specified elements.
   *
   * <p>This implementation is highly efficient when {@code elements} is itself a {@link Multiset}.
   *
   * @param elements the elements that the multiset should contain
   * @throws IllegalArgumentException if {@code elements} is empty
   */
  public static <E extends Enum<E>> EnumMultiset<E> create(Iterable<E> elements) {
    Iterator<E> iterator = elements.iterator();
    EnumMultiset<E> multiset = new EnumMultiset<>(iterator.next().getDeclaringClass());
    return multiset;
  }

  /**
   * Returns a new {@code EnumMultiset} instance containing the given elements. Unlike {@link
   * EnumMultiset#create(Iterable)}, this method does not produce an exception on an empty iterable.
   *
   * @since 14.0
   */
  public static <E extends Enum<E>> EnumMultiset<E> create(Iterable<E> elements, Class<E> type) {
    EnumMultiset<E> result = create(type);
    return result;
  }

  private transient Class<E> type;
  private transient E[] enumConstants;
  private transient int[] counts;
  private transient int distinctElements;
  private transient long size;

  /** Creates an empty {@code EnumMultiset}. */
  private EnumMultiset(Class<E> type) {
    this.type = type;
    this.enumConstants = type.getEnumConstants();
    this.counts = new int[enumConstants.length];
  }

  private boolean isActuallyE(Object o) {
    if (o instanceof Enum) {
      Enum<?> e = (Enum<?>) o;
      int index = e.ordinal();
      return index < enumConstants.length && enumConstants[index] == e;
    }
    return false;
  }

  /**
   * Returns {@code element} cast to {@code E}, if it actually is a nonnull E. Otherwise, throws
   * either a NullPointerException or a ClassCastException as appropriate.
   */
  private void checkIsE(Object element) {
    if (!isActuallyE(element)) {
      throw new ClassCastException("Expected an " + type + " but got " + element);
    }
  }

  int distinctElements() {
    return distinctElements;
  }

  public @NonNegative int size() {
    return (int) size;
  }

  public @NonNegative int count(@UnknownSignedness Object element) {
    // isActuallyE checks for null, but we check explicitly to help nullness checkers.
    if (element == null || !isActuallyE(element)) {
      return 0;
    }
    Enum<?> e = (Enum<?>) element;
    return counts[e.ordinal()];
  }

  // Modification Operations

  public int add(E element, int occurrences) {
    checkIsE(element);
    if (occurrences == 0) {
      return count(element);
    }
    int index = element.ordinal();
    int oldCount = counts[index];
    long newCount = (long) oldCount + occurrences;
    counts[index] = (int) newCount;
    if (oldCount == 0) {
      distinctElements++;
    }
    size += occurrences;
    return oldCount;
  }

  // Modification Operations

  public int remove(Object element, int occurrences) {
    // isActuallyE checks for null, but we check explicitly to help nullness checkers.
    if (element == null || !isActuallyE(element)) {
      return 0;
    }
    Enum<?> e = (Enum<?>) element;
    if (occurrences == 0) {
      return count(element);
    }
    int index = e.ordinal();
    int oldCount = counts[index];
    if (oldCount == 0) {
      return 0;
    } else if (oldCount <= occurrences) {
      counts[index] = 0;
      distinctElements--;
      size -= oldCount;
    } else {
      counts[index] = oldCount - occurrences;
      size -= occurrences;
    }
    return oldCount;
  }

  // Modification Operations

  public int setCount(E element, int count) {
    checkIsE(element);
    int index = element.ordinal();
    int oldCount = counts[index];
    counts[index] = count;
    size += count - oldCount;
    if (oldCount == 0 && count > 0) {
      distinctElements++;
    } else if (oldCount > 0 && count == 0) {
      distinctElements--;
    }
    return oldCount;
  }

  public void clear() {
    Arrays.fill(counts, 0);
    size = 0;
    distinctElements = 0;
  }

  abstract class Itr<T> implements Iterator<T> {
    int index = 0;
    int toRemove = -1;

    abstract T output(int index);

    public boolean hasNext() {
      for (; index < enumConstants.length; index++) {
        if (counts[index] > 0) {
          return true;
        }
      }
      return false;
    }

    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      T result = output(index);
      toRemove = index;
      index++;
      return result;
    }

    public void remove() {
      if (counts[toRemove] > 0) {
        distinctElements--;
        size -= counts[toRemove];
        counts[toRemove] = 0;
      }
      toRemove = -1;
    }
  }

  Iterator<E> elementIterator() {
    return new Itr<E>() {

      E output(int index) {
        return enumConstants[index];
      }
    };
  }

  public void forEachEntry(ObjIntConsumer<? super E> action) {
    for (int i = 0; i < enumConstants.length; i++) {
      if (counts[i] > 0) {
        action.accept(enumConstants[i], counts[i]);
      }
    }
  }
}
