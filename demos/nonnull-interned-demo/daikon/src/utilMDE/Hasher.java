package utilMDE;

/**
 * Hasher is intended to work like Comparable:  it is an optional argument
 * to a hashing data structure (such as a HashSet, HashMap, or WeakHashMap)
 * which specifies the hashCode() and equals() methods.
 *
 * If no Hasher is provided, then clients should act as if the following
 * Hasher were provided:
 * <pre>
 *   class DefaultHasher {
 *     int hashCode(Object o) { return o.hashCode(); }
 *     boolean equals(Object o, Object o2) { return o.equals(o2); }
 *   }
 * </pre>
 **/
public interface Hasher {
  /** hashCode function for objects under consideration (not for Hasher objects). */
  int hashCode(Object o);
  /** The equality function over the objects being hashed. */
  boolean equals(Object o, Object o2);


  /**
   * Equality testing over Hashers.  Has nothing to do with testing
   * the objects being hashed for equality.
   **/
  boolean equals(Object other_hasher);
}
