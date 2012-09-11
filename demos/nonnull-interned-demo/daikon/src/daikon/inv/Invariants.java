package daikon.inv;

import daikon.*;
import java.util.*;

// The downside of this extending Vector is that the operations
// return Objects rather than Invariants.

/**
 * This is essentially a collection of Invariant objects, but with a few
 * convenience methods.
 **/
public final class Invariants
  extends ArrayList<Invariant>
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  public Invariants() {
    super();
  }

  public Invariants(int initialCapacity) {
    super(initialCapacity);
  }

  /**
   * Copy constructor.
   **/
  public Invariants(Invariants arg) {
    super(arg);
  }

  // Override superclass implementation
  public boolean remove(Object o) {
    boolean result = super.remove(o);
    // Perhaps trim to size.
    // The test I really want is "if size() < capacity()/2".
    // But I don't have a way of determining the capacity.
    // Instead, determine whether the size is a power of 2.
    if (result && isPowerOfTwo(size())) {
      trimToSize();
    }
    return result;
  }

  // Remove all the invariants in toRemove. This is faster than
  // repeatedly calling remove(), if toRemove is long.
  public int removeMany(List<Invariant> toRemove) {
    // System.out.printf ("removeMany in %s\n", this.getClass());
    HashSet<Invariant> removeSet = new HashSet<Invariant>(toRemove);
    ArrayList<Invariant> copy = new ArrayList<Invariant>();
    for (Invariant inv : this) {
      if (!removeSet.contains(inv)) {
        copy.add(inv);
        //System.out.printf ("NOT remove set [%x-%x-%x] %s [%s]\n",
        //                   System.identityHashCode (inv), inv.hashCode(),
        //                   ((Object)inv).hashCode(), inv, inv.getClass());
      } else {
        // System.out.printf (" in remove set [%x] %s [%s]\n",
        //                   System.identityHashCode (inv), inv, inv.getClass());
      }
    }
    int numRemoved = size() - copy.size();
    clear();
    addAll(copy);
    return numRemoved;
  }

  // Works for non-negative
  private static final boolean isPowerOfTwo(int x) {
    if (x == 0)
      return true;
    // If x is a power of two, then x - 1 has no bits in common with x
    // OTOH, if x is not a power of two, then x and x - 1 have the same
    // most-significant bit set, so they have at least one bit in common.
    return (x & (x - 1)) == 0;
  }

  /// For testing
  // private static final boolean isPowerOfTwoSlow(int x) {
  //   for (int i=0; true; i++) {
  //     int pow = utilMDE.MathMDE.pow(2, i);
  //     if (pow == x)
  //       return true;
  //     if (pow > x)
  //       return false;
  //   }
  // }
  // public static void main(String[] args) {
  //   for (int i=1; i<10000; i++) {
  //     if (isPowerOfTwo(i) != isPowerOfTwoSlow(i)) {
  //       throw new Error("" + i);
  //     }
  //   }
  // }

}
