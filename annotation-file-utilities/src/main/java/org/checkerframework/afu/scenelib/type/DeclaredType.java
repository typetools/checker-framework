package org.checkerframework.afu.scenelib.type;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * A Java type with optional type parameters and inner type. For example:
 *
 * <pre>
 *   <em>type</em>
 *   <em>type</em>&lt;<em>type parameters</em>&gt;.<em>inner type</em>
 * </pre>
 *
 * A {@code DeclaredType} can represent a wildcard by using "?" as the {@code name}. If this type is
 * a wildcard, it is illegal to call {@link #addTypeParameter(Type)}, {@link
 * #getTypeParameter(int)}, {@link #getTypeParameters()}, {@link #setInnerType(DeclaredType)},
 * {@link #getInnerType()}, and {@link #setTypeParameters(List)}. If called, an {@link
 * IllegalStateException} will be thrown.
 *
 * <p>Types are stored with the outer most type at the top of the type tree. This is opposite to the
 * way types are stored in javac.
 */
public class DeclaredType extends Type {

  /** The {@code name} of a wildcard type. */
  public static final String WILDCARD = "?";

  /** The raw, un-annotated name of this type. "?" for a wildcard. */
  private String name;

  /** The type parameters to this type. Empty if there are none. */
  private List<Type> typeParameters;

  /** The inner type of this type. {@code null} if there is none. */
  private DeclaredType innerType;

  /**
   * Creates a new declared type with no type parameters or inner type.
   *
   * @param name the raw, un-annotated name of this type, or "?" for a wildcard
   */
  public DeclaredType(String name) {
    super();
    this.name = name;
    this.typeParameters = new ArrayList<Type>();
    this.innerType = null;
  }

  /** Creates a new declared type with an empty name and no type parameters or inner type. */
  public DeclaredType() {
    this("");
  }

  /**
   * Sets the raw, un-annotated name of this type.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets the raw, un-annotated name of this type.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Adds the given type parameter to this type.
   *
   * @param typeParameter the type parameter
   */
  public void addTypeParameter(Type typeParameter) {
    checkWildcard();
    typeParameters.add(typeParameter);
  }

  /**
   * Sets the type parameters of this type.
   *
   * @param typeParameters the type parameters
   */
  public void setTypeParameters(List<Type> typeParameters) {
    checkWildcard();
    this.typeParameters = typeParameters;
  }

  /**
   * Gets the type parameter at the given index.
   *
   * @param index the index
   * @return the type parameter
   */
  public Type getTypeParameter(int index) {
    checkWildcard();
    return typeParameters.get(index);
  }

  /**
   * Gets a copy of the type parameters of this type. This will be empty if there are none.
   *
   * @return the type parameters
   */
  public List<Type> getTypeParameters() {
    checkWildcard();
    return new ArrayList<Type>(typeParameters);
  }

  /**
   * Sets the inner type.
   *
   * @param innerType the inner type
   */
  public void setInnerType(DeclaredType innerType) {
    checkWildcard();
    this.innerType = innerType;
  }

  /**
   * Gets the inner type. This will be {@code null} if there is none.
   *
   * @return the inner type or {@code null}
   */
  public DeclaredType getInnerType() {
    checkWildcard();
    return innerType;
  }

  @Override
  public Kind getKind() {
    return Kind.DECLARED;
  }

  /**
   * Determines if this type is a wildcard.
   *
   * @return {@code true} if this type is a wildcard, {@code false} otherwise
   */
  public boolean isWildcard() {
    return WILDCARD.equals(name);
  }

  /** Throw an {@link IllegalStateException} if this type is a wildcard. */
  private void checkWildcard() {
    if (isWildcard()) {
      throw new IllegalStateException(
          "This method can't be called " + "since this DeclaredType is a wildcard.");
    }
  }

  @Override
  public String toString() {
    StringJoiner result = new StringJoiner(", ", "DeclaredType[", "]");
    result.add("name=" + name);
    if (!typeParameters.isEmpty()) {
      result.add("typeParameters=" + typeParameters);
    }
    if (innerType != null) {
      result.add("innerType=" + innerType);
    }
    return result.toString();
  }
}
