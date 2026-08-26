package org.checkerframework.framework.type;

import java.util.Objects;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeScanner;

/**
 * Computes the hashcode of an AnnotatedTypeMirror using the underlying type and primary annotations
 * and the hash code of component types of AnnotatedTypeMirror.
 *
 * <p>This class should be synchronized with EqualityAtmComparer.
 *
 * @see org.checkerframework.framework.type.EqualityAtmComparer for more details.
 *     <p>This is used by AnnotatedTypeMirror.hashCode.
 */
public class HashcodeAtmVisitor extends SimpleAnnotatedTypeScanner<Integer, Void> {

  /** Creates a {@link HashcodeAtmVisitor}. */
  public HashcodeAtmVisitor() {
    // Plain addition is a weak combiner: it is order-insensitive, so component types visited in
    // different structural positions can cancel out or coincide, causing many unequal
    // AnnotatedTypeMirrors to hash to the same value. That collapses HashMap-based caches (e.g.
    // SubtypeVisitHistory) keyed on AnnotatedTypeMirror into a few huge buckets, making lookups
    // that should be O(1) amortized cost O(bucket size) instead.
    super((r1, r2) -> r1 * 31 + r2, 0);
  }

  /**
   * Generates hashcode for type using the underlying type and the primary annotation. This method
   * does not descend into component types (this occurs in the scan method)
   *
   * @param type the type
   */
  @Override
  protected Integer defaultAction(AnnotatedTypeMirror type, Void v) {
    // To differentiate between partially initialized type's (which may have null components)
    // and fully initialized types, null values are allowed
    if (type == null) {
      return 0;
    }
    return Objects.hash(type.getUnderlyingTypeHashCode(), type.getPrimaryAnnotations().toString());
  }
}
