import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.checkerframework.checker.nonempty.qual.EnsuresNonEmptyIf;
import org.checkerframework.checker.nonempty.qual.PolyNonEmpty;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class SizeInIsEmpty<E extends Object> extends AbstractSet<E> {

  @SideEffectFree
  public SizeInIsEmpty() {}

  // Query Operations

  @Pure
  @Override
  public int size() {
    return -1;
  }

  @Pure
  @Override
  @EnsuresNonEmptyIf(result = false, expression = "this")
  public boolean isEmpty() {
    if (size() == 0) {
      return true;
    } else {
      return false;
    }
  }

  @EnsuresNonEmptyIf(result = false, expression = "this")
  public boolean isEmpty2() {
    // :: error: contracts.conditional.postcondition
    return size() == 0 ? true : false;
  }

  Set<Object> set = new HashSet<>();

  @EnsuresNonEmptyIf(result = false, expression = "this.set")
  public boolean isEmpty2B() {
    // TODO: this is a false positive
    // :: error: contracts.conditional.postcondition
    return set.size() == 0 ? true : false;
  }

  @EnsuresNonEmptyIf(result = false, expression = "this")
  public boolean isEmpty3() {
    return size() == 0;
  }

  // iterators

  @Override
  public @PolyNonEmpty Iterator<E> iterator(@PolyNonEmpty SizeInIsEmpty<E> this) {
    throw new Error("");
  }

  void testRefineIsEmpty1(SizeInIsEmpty<Object> container) {
    if (!container.isEmpty()) {
      container.iterator().next();
    } else {
      // :: error: (method.invocation)
      container.iterator().next();
    }
  }

  void testRefineIsEmpty2(SizeInIsEmpty<Object> container) {
    if (!container.isEmpty2()) {
      container.iterator().next();
    } else {
      // :: error: (method.invocation)
      container.iterator().next();
    }
  }

  void testRefineIsEmpty3(SizeInIsEmpty<Object> container) {
    if (!container.isEmpty3()) {
      container.iterator().next();
    } else {
      // :: error: (method.invocation)
      container.iterator().next();
    }
  }
}
