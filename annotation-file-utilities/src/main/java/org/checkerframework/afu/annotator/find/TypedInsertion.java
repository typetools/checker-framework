package org.checkerframework.afu.annotator.find;

import java.util.List;
import org.checkerframework.afu.scenelib.type.ArrayType;
import org.checkerframework.afu.scenelib.type.BoundedType;
import org.checkerframework.afu.scenelib.type.DeclaredType;
import org.checkerframework.afu.scenelib.type.Type;

/**
 * An insertion that may result in code generation other than just annotations. {@code
 * TypedInsertion}s keep track of insertions on inner types. If there is no type given in the
 * source, one may be generated (along with other code necessary in the context) to serve as an
 * insertion site.
 *
 * <p>We don't know until the end of the whole insertion process whether the type already exists or
 * not. To remedy this, we store a reference to each insertion on an inner type of a receiver in two
 * places: the global list of all insertions and the {@code TypedInsertion} that is the parent of
 * the inner type insertion. If the type is not already present, the inner type insertions are
 * inserted into the new type and labeled as "inserted" (with {@link
 * Insertion#setInserted(boolean)}) so they are not inserted as the rest of the insertions list is
 * processed.
 */
public abstract class TypedInsertion extends Insertion {
  /** The type for insertion. */
  protected Type type;

  /** If true only the annotations from {@link #type} will be inserted. */
  protected boolean annotationsOnly;

  /** The inner types to go on this insertion. See {@link ReceiverInsertion} for more details. */
  protected List<Insertion> innerTypeInsertions;

  public TypedInsertion(Type type, Criteria criteria, List<Insertion> innerTypeInsertions) {
    this(type, criteria, false, innerTypeInsertions);
  }

  public TypedInsertion(
      Type type, Criteria criteria, boolean b, List<Insertion> innerTypeInsertions) {
    super(criteria, b);
    this.type = type;
    this.innerTypeInsertions = innerTypeInsertions;
    annotationsOnly = false;
  }

  /**
   * If {@code true} only the annotations on {@code type} will be inserted. This is useful when the
   * "new" has already been inserted.
   */
  public void setAnnotationsOnly(boolean annotationsOnly) {
    this.annotationsOnly = annotationsOnly;
  }

  /** Sets the type. */
  public void setType(Type type) {
    this.type = type;
  }

  /**
   * Gets the type. It is assumed that the returned value will be modified to update the type to be
   * inserted.
   */
  public Type getType() {
    return type;
  }

  /**
   * Gets the inner type insertions associated with this insertion.
   *
   * @return a copy of the inner types
   */
  public List<Insertion> getInnerTypeInsertions() {
    return innerTypeInsertions;
  }

  public DeclaredType getBaseType() {
    return getBaseType(type);
  }

  public static DeclaredType getBaseType(Type type) {
    switch (type.getKind()) {
      case DECLARED:
        return (DeclaredType) type;
      case BOUNDED:
        return getBaseType(((BoundedType) type).getName());
      case ARRAY:
        return getBaseType(((ArrayType) type).getComponentType());
      default: // should never be reached
        return null;
    }
  }
}
