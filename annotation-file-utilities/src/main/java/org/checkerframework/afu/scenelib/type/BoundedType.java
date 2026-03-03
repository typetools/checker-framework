package org.checkerframework.afu.scenelib.type;

import java.util.List;
import java.util.Locale;

/**
 * A Java bounded type. For example:
 *
 * <pre>
 *   K extends Object
 *   E super String
 *   ? super String
 * </pre>
 *
 * Calling {@link #addAnnotation(String)}, {@link #getAnnotation(int)}, or {@link #getAnnotations()}
 * on a {@code BoundedType} will result in an {@link UnsupportedOperationException}. Annotations
 * should be added to the {@code name} and {@code bound} of this {@code BoundedType}.
 */
public class BoundedType extends Type {

  /** The possible bound kinds. */
  public enum BoundKind {
    EXTENDS,
    SUPER;

    /** Gets this bound kind in a format that can be inserted into source code. */
    @Override
    public String toString() {
      return super.toString().toLowerCase(Locale.getDefault());
    }
  }

  /**
   * The type name. For example, 'K' in:
   *
   * <pre>
   *   K extends Object
   * </pre>
   */
  private DeclaredType name;

  /** The bound kind. */
  private BoundKind boundKind;

  /**
   * The bound of this type. For example, 'Object' in:
   *
   * <pre>
   *   K extends Object
   * </pre>
   */
  private DeclaredType bound;

  /**
   * Creates a new bounded type.
   *
   * @param name the type variable name
   * @param boundKind the bound kind
   * @param bound the bound
   */
  public BoundedType(DeclaredType name, BoundKind boundKind, DeclaredType bound) {
    super();
    this.name = name;
    this.boundKind = boundKind;
    this.bound = bound;
  }

  private static BoundKind javacBoundKindToBoundKind(com.sun.tools.javac.code.BoundKind boundKind) {
    switch (boundKind) {
      case EXTENDS:
        return BoundKind.EXTENDS;
      case SUPER:
        return BoundKind.SUPER;
      default:
        throw new RuntimeException();
    }
  }

  /**
   * Creates a new bounded type.
   *
   * @param name the type variable name
   * @param boundKind the bound kind
   * @param bound the bound
   */
  public BoundedType(
      DeclaredType name, com.sun.tools.javac.code.BoundKind boundKind, DeclaredType bound) {
    this(name, javacBoundKindToBoundKind(boundKind), bound);
  }

  /**
   * Gets the type variable name. For example, 'K' in:
   *
   * <pre>
   *   K extends Object
   * </pre>
   *
   * @return the type variable name
   */
  public DeclaredType getName() {
    return name;
  }

  /**
   * Gets the bound of this type.
   *
   * @return the bound
   */
  public Type getBound() {
    return bound;
  }

  /**
   * Gets the bound kind of this type.
   *
   * @return the bound kind
   */
  public BoundKind getBoundKind() {
    return boundKind;
  }

  @Override
  public Kind getKind() {
    return Kind.BOUNDED;
  }

  // Override Type methods and throw an exception since annotations can not be
  // put on a bounded type. Annotations should be added to the "type" and
  // "bound" of a bounded type.

  @Override
  public void addAnnotation(String annotation) {
    throw new UnsupportedOperationException("BoundedType cannot have annotations.");
  }

  @Override
  public String getAnnotation(int index) {
    throw new UnsupportedOperationException("BoundedType cannot have annotations.");
  }

  @Override
  public List<String> getAnnotations() {
    throw new UnsupportedOperationException("BoundedType cannot have annotations.");
  }
}
